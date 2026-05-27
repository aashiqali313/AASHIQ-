import os
import sys

def encrypt_decrypt_file(input_path, output_path):
    """
    Encrypts or decrypts a file using high-performance symmetric XOR operation with key 0xAE.
    Since XOR is symmetric, running this script on .mp4 yields .aashiq, and running it again yields .mp4.
    """
    key = 0xAE
    
    # 1MB buffer size for memory-efficient processing of large video files
    buffer_size = 1024 * 1024 
    
    print(f"[*] Processing: {input_path}")
    print(f"[*] Output Target: {output_path}")
    
    try:
        total_size = os.path.getsize(input_path)
        processed_size = 0
        
        with open(input_path, 'rb') as f_in, open(output_path, 'wb') as f_out:
            while True:
                chunk = f_in.read(buffer_size)
                if not chunk:
                    break
                
                # Apply XOR key to each byte dynamically
                encrypted_chunk = bytearray(b ^ key for b in chunk)
                f_out.write(encrypted_chunk)
                
                processed_size += len(chunk)
                percent = (processed_size / total_size) * 100
                print(f"[>] Progress: {percent:.2f}% ({processed_size}/{total_size} bytes)", end='\r')
                
        print("\n[+] Successfully converted file!")
        print(f"[+] Save location: {os.path.abspath(output_path)}")
        print("[!] Note: Put this '.aashiq' file in your course directory on your mobile and import again!")
    except Exception as e:
        print(f"\n[-] Error occurred: {e}")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("======================================================")
        print("|  Aashiq Video Encryption Engine (.aashiq Secure Pack) |")
        print("======================================================")
        print("Usage:")
        print("   python encrypt_course.py <path_to_video_file> [output_path]")
        print("\nExamples:")
        print("   python encrypt_course.py my_lecture.mp4")
        print("   (Creates 'my_lecture.aashiq' in the same folder)")
        sys.exit(1)
        
    in_file = sys.argv[1]
    if not os.path.exists(in_file):
        print(f"[-] Error: Input file '{in_file}' not found.")
        sys.exit(1)
        
    if len(sys.argv) >= 3:
        out_file = sys.argv[2]
    else:
        # Default conversion from .mp4 to .aashiq or vice-versa
        base, ext = os.path.splitext(in_file)
        if ext.lower() == ".aashiq":
            out_file = base + "_decrypted.mp4"
        else:
            out_file = base + ".aashiq"
            
    encrypt_decrypt_file(in_file, out_file)
