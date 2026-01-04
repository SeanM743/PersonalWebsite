#!/bin/bash
cd "$(dirname "$0")"

# Get the classpath
CLASSPATH="target/classes"
for jar in ~/.m2/repository/org/springframework/boot/spring-boot-starter-web/3.3.0/*.jar; do
    CLASSPATH="$CLASSPATH:$jar"
done
for jar in ~/.m2/repository/org/springframework/boot/spring-boot/3.3.0/*.jar; do
    CLASSPATH="$CLASSPATH:$jar"
done
for jar in ~/.m2/repository/org/springframework/spring-*/6.1.8/*.jar; do
    CLASSPATH="$CLASSPATH:$jar"
done

# Add all dependencies from .m2 repository
for jar in $(find ~/.m2/repository -name "*.jar" 2>/dev/null | head -100); do
    CLASSPATH="$CLASSPATH:$jar"
done

echo "Starting application with classpath: $CLASSPATH"
java -cp "$CLASSPATH" com.personal.backend.BackendApplication