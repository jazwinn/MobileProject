import subprocess
import os

def run_build():
    try:
        # Run gradle and capture output
        process = subprocess.Popen(['./gradlew', 'assembleDebug'], 
                                   stdout=subprocess.PIPE, 
                                   stderr=subprocess.STDOUT)
        
        output, _ = process.communicate()
        
        # Decode handles utf-8/utf-16/ascii
        try:
            text = output.decode('utf-8')
        except:
            try:
                text = output.decode('utf-16')
            except:
                text = output.decode('latin-1')
        
        # Find errors
        lines = text.splitlines()
        for line in lines:
            if "error:" in line.lower() or "failed" in line.lower() or "exception" in line.lower():
                print(line)
                
    except Exception as e:
        print(f"Script failed: {e}")

if __name__ == "__main__":
    run_build()
