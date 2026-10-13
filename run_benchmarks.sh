#!/data/data/com.termux/files/usr/bin/bash
cd ~/needle3-test

inputs=(
  "Create a task to buy milk"
  "Mark task 4 complete"
  "I already finished task 4"
  "Do the thing"
  "What's the weather?"
)

echo "=== NEEDLE 3 BENCHMARK RUN ==="
for input in """"; do
  echo "----------------------------------------"
  echo "INPUT: $input"
  start_time=$(date +%s%N)
  output=$(./needle --model needle3.cact --tools test_tools.json --tool-index test_tools.idx --prompt "$input" --max 100 --threads 4 2>&1)
  end_time=$(date +%s%N)
  elapsed_ms=$(( (end_time - start_time) / 1000000 ))
  echo "OUTPUT: $output"
  echo "ELAPSED_MS: $elapsed_ms ms"
done
