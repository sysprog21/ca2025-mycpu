static void quicksort(int *arr, int l, int r)
{
    if (l >= r)
        return;

    int pivot = arr[l];
    int i = l, j = r;
    while (i < j) {
        while (arr[j] >= pivot && i < j)
            --j;
        arr[i] = arr[j];
        while (arr[i] < pivot && i < j)
            ++i;
        arr[j] = arr[i];
    }
    arr[i] = pivot;
    quicksort(arr, l, i - 1);
    quicksort(arr, i + 1, r);
}

int main()
{
    int nums[10] = {6, 2, 4, 5, 3, 1, 0, 9, 7, 8};

    quicksort(nums, 0, 9);

    for (int i = 1; i <= 10; ++i)
        *(volatile int *) (i * 4) = nums[i - 1];

    return 0;
}
