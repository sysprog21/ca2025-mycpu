// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

int fib(int a)
{
    if (a == 1 || a == 2)
        return 1;
    return fib(a - 1) + fib(a - 2);
}

int main()
{
    *(int *) (4) = fib(10);
}