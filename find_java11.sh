#!/bin/bash
# Run this once before building if you hit "requires Java 11" error
# It finds Java 11 on your system and patches gradle.properties

PROPS="$(dirname "$0")/gradle.properties"

# Common Java 11 locations
CANDIDATES=(
    "/usr/lib/jvm/java-11-openjdk-amd64"
    "/usr/lib/jvm/java-11-openjdk"
    "/usr/lib/jvm/java-11"
    "/usr/local/openjdk-11"
    "/opt/java/jdk-11"
    "/opt/java/11"
    "/usr/lib/jvm/temurin-11"
    "/Library/Java/JavaVirtualMachines/temurin-11.jdk/Contents/Home"
)

FOUND=""
for path in "${CANDIDATES[@]}"; do
    if [ -f "$path/bin/java" ]; then
        FOUND="$path"
        break
    fi
done

# Also try java_home command (macOS / some Linux)
if [ -z "$FOUND" ]; then
    FOUND=$(java_home -v 11 2>/dev/null || true)
fi

# Try find as last resort
if [ -z "$FOUND" ]; then
    FOUND=$(find /usr /opt /Library -name "java" -path "*/11*/bin/java" 2>/dev/null | head -1 | sed 's|/bin/java||')
fi

if [ -z "$FOUND" ]; then
    echo "ERROR: Java 11 not found. Install it with:"
    echo "  sudo apt install openjdk-11-jdk"
    exit 1
fi

echo "Found Java 11 at: $FOUND"

# Remove any existing java.home line and add new one
grep -v "org.gradle.java.home" "$PROPS" > "$PROPS.tmp"
echo "org.gradle.java.home=$FOUND" >> "$PROPS.tmp"
mv "$PROPS.tmp" "$PROPS"

echo "Patched gradle.properties:"
grep "java.home" "$PROPS"
echo "Now run your build."
