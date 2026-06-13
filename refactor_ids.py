import os
import re

BASE_DIR = r"c:\Users\BackEndYS\Desktop\ALL FOLDER Desktop\diBimbing\0. FINAL PROJECT\medibookAPI\src\main"

def process_java_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    original = content

    # 1. JPA Repository
    content = re.sub(r'JpaRepository<([a-zA-Z0-9_]+),\s*Long>', r'JpaRepository<\1, String>', content)

    # 2. Entity @Id
    content = re.sub(r'@GeneratedValue\(strategy\s*=\s*GenerationType\.IDENTITY\)', r'@GeneratedValue(strategy = GenerationType.UUID)', content)
    
    # 3. Fields & Parameters
    patterns = [
        (r'\bLong\s+id\b', 'String id'),
        (r'\bLong\s+userId\b', 'String userId'),
        (r'\bLong\s+doctorId\b', 'String doctorId'),
        (r'\bLong\s+patientId\b', 'String patientId'),
        (r'\bLong\s+serviceId\b', 'String serviceId'),
        (r'\bLong\s+scheduleId\b', 'String scheduleId'),
        (r'\bLong\s+bookingId\b', 'String bookingId'),
        (r'ResponseEntity<([a-zA-Z0-9_<>]+)>\s*([a-zA-Z0-9_]+)\(\s*@PathVariable\("[a-zA-Z0-9_]+"([^)]*)\)\s*Long\s+([a-zA-Z0-9_]+)', 
         r'ResponseEntity<\1> \2(@PathVariable("\3") String \4'),
        (r'Long\s+([a-zA-Z0-9_]+)Id\b', r'String \1Id') # Generic fallback for xxxId
    ]
    for pattern, repl in patterns:
        content = re.sub(pattern, repl, content)

    # 4. Return types for getters/setters (if any hardcoded Long returned)
    content = re.sub(r'public\s+Long\s+get([a-zA-Z0-9_]+)Id\(\)', r'public String get\1Id()', content)
    content = re.sub(r'public\s+void\s+set([a-zA-Z0-9_]+)Id\(Long\s+', r'public void set\1Id(String ', content)
    content = re.sub(r'public\s+Long\s+getId\(\)', r'public String getId()', content)
    content = re.sub(r'public\s+void\s+setId\(Long\s+', r'public void setId(String ', content)

    if original != content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Updated Java: {filepath}")

def process_yaml_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    original = content

    # Replace BIGINT with VARCHAR(36)
    content = re.sub(r'type:\s*BIGINT(?:\(\d+\))?', 'type: VARCHAR(36)', content)
    # Remove autoIncrement: true
    content = re.sub(r'\s*autoIncrement:\s*true', '', content)

    if original != content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Updated YAML: {filepath}")

def main():
    for root, dirs, files in os.walk(BASE_DIR):
        for file in files:
            filepath = os.path.join(root, file)
            if filepath.endswith('.java'):
                process_java_file(filepath)
            elif filepath.endswith('.yaml') or filepath.endswith('.yml'):
                if 'changelog' in filepath:
                    process_yaml_file(filepath)

if __name__ == "__main__":
    main()
