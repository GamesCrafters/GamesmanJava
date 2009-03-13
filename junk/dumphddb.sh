#!/bin/zsh

for dir in gjhadoop_*
do
	dd if=$dir/slice count=$[$(wc -c < $dir/slice) - 8] bs=1 2>/dev/null
done

