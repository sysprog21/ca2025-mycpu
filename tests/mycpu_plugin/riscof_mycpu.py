import os
import shutil
import subprocess
import logging
import threading

import riscof.utils as utils
from riscof.pluginTemplate import pluginTemplate

logger = logging.getLogger()

class mycpu(pluginTemplate):
    __model__ = "mycpu"
    __version__ = "0.1.0"

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        config = kwargs.get('config')

        if config is None:
            print("Please provide configuration")
            raise SystemExit(1)

        self.num_jobs = str(config['jobs'] if 'jobs' in config else 1)
        self.pluginpath = os.path.abspath(config['pluginpath'])
        self.isa_spec = os.path.abspath(config['ispec'])
        self.platform_spec = os.path.abspath(config['pspec'])

        # Path to MyCPU project (can be 1-single-cycle, 2-mmio-trap, or 3-pipeline)
        self.mycpu_project = os.path.abspath(config['PATH'])

        if 'target_run' in config and config['target_run'] == '0':
            self.target_run = False
        else:
            self.target_run = True

    def initialise(self, suite, work_dir, archtest_env):
        self.work_dir = work_dir
        self.suite_dir = suite

        # Try to find RISC-V GCC (support multiple naming conventions)
        riscv_prefix = None
        for prefix in ['riscv32-unknown-elf', 'riscv-none-elf', 'riscv64-unknown-elf']:
            if shutil.which(f'{prefix}-gcc'):
                riscv_prefix = prefix
                break

        if not riscv_prefix:
            raise RuntimeError("RISC-V GCC not found. Tried: riscv32-unknown-elf-gcc, riscv-none-elf-gcc")

        # Compile command for RISC-V tests
        self.compile_cmd = (f'{riscv_prefix}-gcc -march={{0}} '
            '-static -mcmodel=medany -fvisibility=hidden -nostdlib -nostartfiles '
            f'-T {self.pluginpath}/env/link.ld '
            f'-I {self.pluginpath}/env/ '
            f'-I {archtest_env} {{1}} -o {{2}} {{3}}')
        self.riscv_objcopy = f'{riscv_prefix}-objcopy'

    def build(self, isa_yaml, platform_yaml):
        ispec = utils.load_yaml(isa_yaml)['hart0']
        self.xlen = ('64' if 64 in ispec['supported_xlen'] else '32')
        self.isa = 'rv' + self.xlen

        # Standard single-letter extensions
        if "I" in ispec["ISA"]:
            self.isa += 'i'
        if "M" in ispec["ISA"]:
            self.isa += 'm'
        if "A" in ispec["ISA"]:
            self.isa += 'a'
        if "F" in ispec["ISA"]:
            self.isa += 'f'
        if "D" in ispec["ISA"]:
            self.isa += 'd'
        if "C" in ispec["ISA"]:
            self.isa += 'c'

        # Z-extensions (Zicsr, Zifencei, etc.)
        if "Zicsr" in ispec["ISA"]:
            self.isa += '_zicsr'
        if "Zifencei" in ispec["ISA"]:
            self.isa += '_zifencei'

        self.compile_cmd = self.compile_cmd + f' -mabi=' + ('lp64 ' if 64 in ispec['supported_xlen'] else 'ilp32 ') + f'-DXLEN={self.xlen} '
        logger.debug(f'Compile command template: {self.compile_cmd}')

    def runTests(self, testList):
        total_tests = len(testList)
        logger.info(f'=== BATCH MODE: Preparing {total_tests} tests ===')

        # Phase 1: Compile all tests and prepare .asmbin files
        test_metadata = []
        test_num = 0
        for testname in testList:
            test_num += 1
            testentry = testList[testname]
            test = testentry['test_path']
            test_dir = testentry['work_dir']

            logger.info(f'Compiling test {test_num}/{total_tests}: {testname}')

            elf = os.path.join(test_dir, 'dut.elf')
            sig_file = os.path.join(test_dir, 'DUT-mycpu.signature')
            asmbin = os.path.join(test_dir, 'test.asmbin')

            # Compile test to ELF
            # Force Zicsr extension since test harness uses CSR instructions
            test_isa = testentry['isa'].lower()
            if 'zicsr' not in test_isa and 'rv32' in test_isa:
                test_isa += '_zicsr'
            compile_cmd = self.compile_cmd.format(test_isa, test, elf, '')

            logger.debug('Compiling test: ' + compile_cmd)
            utils.shellCommand(compile_cmd).run(cwd=test_dir)

            # Verify ELF was created
            if not os.path.exists(elf):
                logger.error(f'ELF compilation failed: {elf} not created')
                continue

            # Convert ELF to asmbin format for MyCPU
            objcopy_cmd = f'{self.riscv_objcopy} -O binary {elf} {asmbin}'
            logger.debug('Converting to asmbin: ' + objcopy_cmd)
            utils.shellCommand(objcopy_cmd).run(cwd=test_dir)

            # Store metadata for batch testing
            test_metadata.append({
                'name': testname,
                'elf': elf,
                'sig_file': sig_file,
                'asmbin': asmbin,
                'test_dir': test_dir
            })

        if not self.target_run:
            return

        # Phase 2: Generate batch test file with all tests
        logger.info(f'=== Generating batch test file with {len(test_metadata)} tests ===')
        batch_test_scala = self._generate_batch_test_scala(test_metadata)
        dest_path = os.path.join(self.mycpu_project, 'src/test/scala/riscv/compliance/ComplianceTest.scala')
        os.makedirs(os.path.dirname(dest_path), exist_ok=True)

        with open(dest_path, 'w') as f:
            f.write(batch_test_scala)

        # Phase 3: Run all tests in single SBT invocation
        logger.info(f'=== Running all {len(test_metadata)} tests in single SBT session ===')
        project_dir_name = os.path.basename(self.mycpu_project)
        project_map = {
            '1-single-cycle': 'singleCycle',
            '2-mmio-trap': 'mmioTrap',
            '3-pipeline': 'pipeline'
        }
        sbt_project_name = project_map.get(project_dir_name, 'singleCycle')
        parent_dir = os.path.dirname(self.mycpu_project)

        # Batch log file
        batch_log = os.path.join(self.work_dir, 'batch_test.log')

        # Run all tests with real-time progress feedback
        cmd = f'cd {parent_dir} && sbt --batch "project {sbt_project_name}" "testOnly riscv.compliance.ComplianceTest" 2>&1'
        logger.debug(f'Running batch test: {cmd}')
        timeout_sec = 3600
        try:
            import re
            test_counter = 0

            # Stream SBT output with progress indicators
            proc = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)

            def timeout_handler():
                logger.error(f"Simulation timed out after {timeout_sec} seconds! Killing process...")
                proc.terminate()
                proc.kill()
            
            timer = threading.Timer(timeout_sec, timeout_handler)
            timer.start()

            try:
                with open(batch_log, 'w') as log_file:
                    for line in proc.stdout:
                        log_file.write(line)  # Save to log

                        # Detect test execution and show progress
                        # Match ScalaTest output: "- should pass test <testname>"
                        test_match = re.search(r'should pass test (.+?)[\s\*]', line)
                        if test_match:
                            test_counter += 1
                            test_name = test_match.group(1)
                            logger.info(f'[{test_counter}/{len(test_metadata)}] Running: {test_name}')

                        # Show compilation progress
                        if '[info] Compiling' in line:
                            logger.info('Compiling test suite...')

                        # Show errors immediately
                        if '[error]' in line or 'FAILED' in line:
                            logger.warning(line.strip())
            finally:
                timer.cancel()

            proc.wait()
            logger.info(f'Batch test completed. Full log: {batch_log}')

            # Verify signatures were generated
            success_count = 0
            fail_count = 0
            for meta in test_metadata:
                if os.path.exists(meta['sig_file']):
                    success_count += 1
                else:
                    fail_count += 1
                    logger.warning(f"Signature not created: {meta['name']}")
                    # Create empty signature to allow RISCOF to continue
                    with open(meta['sig_file'], 'w') as f:
                        for i in range(256):
                            f.write('00000000\n')

            logger.info(f'Results: {success_count} passed, {fail_count} failed')

        except Exception as e:
            logger.error(f'Batch test execution failed: {e}')
            logger.error(f'See {batch_log} for detailed error output')
            # Create empty signatures for all missing files
            for meta in test_metadata:
                if not os.path.exists(meta['sig_file']):
                    with open(meta['sig_file'], 'w') as f:
                        for i in range(256):
                            f.write('00000000\n')

        return

    def _generate_test_scala(self, testname, elfFile, sigFile, asmbinFile):
        """Generate Scala test file for this compliance test"""
        return f'''// Auto-generated compliance test
package riscv.compliance

import riscv.TestAnnotations

class ComplianceTest extends ComplianceTestBase {{
  behavior.of("MyCPU Compliance")

  it should "pass test {testname}" in {{
    runComplianceTest(
      "test.asmbin",
      "{elfFile}",
      "{sigFile}",
      TestAnnotations.annos
    )
  }}
}}
'''

    def _generate_batch_test_scala(self, test_metadata):
        """Generate Scala test file with all compliance tests in batch mode"""

        # Copy all .asmbin files to resources directory with unique names
        resource_dir = os.path.join(self.mycpu_project, 'src/main/resources')
        os.makedirs(resource_dir, exist_ok=True)

        # Generate test cases for all tests
        test_cases = []
        for idx, meta in enumerate(test_metadata):
            # Copy asmbin with unique name
            asmbin_name = f"test_{idx:03d}.asmbin"
            shutil.copy(meta['asmbin'], os.path.join(resource_dir, asmbin_name))

            # Generate test case
            test_case = f'''  it should "pass test {meta['name']}" in {{
    runComplianceTest(
      "{asmbin_name}",
      "{meta['elf']}",
      "{meta['sig_file']}",
      TestAnnotations.annos
    )
  }}'''
            test_cases.append(test_case)

        # Combine all test cases into single Scala file
        test_cases_str = '\n\n'.join(test_cases)

        return f'''// Auto-generated batch compliance test
package riscv.compliance

import riscv.TestAnnotations

class ComplianceTest extends ComplianceTestBase {{
  behavior.of("MyCPU Compliance")

{test_cases_str}
}}
'''
