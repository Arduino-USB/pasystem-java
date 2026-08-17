#!/bin/bash

# Configuration
TARGET_DIR="/home/javaClient/javaMount"
LOG_FILE="$TARGET_DIR/bootstap.log"

# Ensure target directory exists
if [ ! -d "$TARGET_DIR" ]; then
    exit 0
fi

# Find all .sh files at the root of the target folder (non-recursive)
for script_path in "$TARGET_DIR"/*.sh; do
    # Check if any .sh file actually exists (handles glob returning literal if none found)
    [ -e "$script_path" ] || continue

    script_name=$(basename "$script_path")
    working_dir="$TARGET_DIR/${script_name}.folder"

    # Determine working directory
    if [ ! -d "$working_dir" ]; then
        working_dir="/tmp"
    fi

    # Capture timestamp
    current_time=$(date "+%Y-%m-%d %H:%M:%S")

    # Temporary files for stdout and stderr
    stdout_file=$(mktemp)
    stderr_file=$(mktemp)

    # Execute the script with the determined working directory
    (
        cd "$working_dir" || exit 1
        bash "$script_path"
    ) > "$stdout_file" 2> "$stderr_file"

    # Read stdout and stderr contents
    stdout_content=$(cat "$stdout_file")
    stderr_content=$(cat "$stderr_file")

    # Clean up temp files
    rm -f "$stdout_file" "$stderr_file"

    # Format output section
    if [ -z "$stdout_content" ]; then
        output_section="No Ouput"
    else
        output_section="$stdout_content"
    fi

    # Append to bootstrap.log
    {
        echo "============================="
        echo "Time: $current_time"
        echo "Invoked $script_name with $working_dir"
        echo -e "Output: \n$output_section"
        echo ""
        
        # Include Script error section only if stderr is not empty
        if [ -s "$stderr_content" ] || [ -n "$stderr_content" ]; then
            # Note: Depending on whether stderr file has content:
            if [ -n "$stderr_content" ]; then
                echo "+++++++++++++++++++++++++++++"
                echo -e "Script error: \n$stderr_content"
                echo ""
            fi
        fi
        echo "+++++++++++++++++++++++++++++" # Wait, let's look closely at the requested log format
    } >> "$LOG_FILE"
done
