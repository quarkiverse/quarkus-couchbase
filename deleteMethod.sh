count_braces() {
    local method="$1"
    local file="$2"

    perl -ne '
        BEGIN { $count = 0; $found = 0; $start_line = 0; }
        if (/\b'"$method"'\s*\(/) { $found = 1; $start_line = $.; }
        if ($found) {
            $count += tr/{/{/;
            $count -= tr/}/}/;
            if ($count == 0 && $found == 1) {
                print "START:$start_line | END:$.\n";
                exit;
            }
        }
    ' "$file"
}

delete_method() {
    local method="$1"
    local file="$2"

    local line_info
    local start_line
    local end_line

    line_info=$(count_braces "$method" "$file")
    start_line=$(echo "$line_info" | cut -d':' -f2 | cut -d' ' -f1)
    end_line=$(echo "$line_info" | cut -d':' -f3)

    if [ -n "$start_line" ] && [ -n "$end_line" ]; then
        start_line=$((start_line - 1))
        sed -i '' "${start_line},${end_line}d" "$file"
        echo "Deleted method '$method' from $file (Lines: $start_line - $end_line)"
    else
        echo "Method '$method' not found in $file"
    fi
}

# Entry point
if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <method_name> <file_name>"
    exit 1
fi

delete_method "$1" "$2"