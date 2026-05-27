# AASHIQ+ Cinematic Offline Learning System (Course Creation Guide)
---

## 📖 PART 1: Course File Ki Structure (कौर्स फ़ाइल का ढांचा)

AASHIQ+ में कौर्स को लोड करने के लिए आपको नीचे दिए गए फ़ोल्डर्स और फ़ाइल स्ट्रक्चर का पालन करना होगा। इस फ़ोल्डर्स को आप अपने फ़ोन में या PC पर बना कर डायरेक्ट इम्पोर्ट कर सकते हैं।

```
📂 My_Premium_Course (Parent Directory)
 ┣ 📜 metadata.json              <-- (Optional: Course title & description)
 ┣ 📂 Chapter_1_Basics (Module)
 ┃ ┣ 📜 Lecture_1.aashiq         <-- Encrypted video (XOR Encrypted)
 ┃ ┣ 📜 Lecture_1.md             <-- Rich Notes with images, markdown & quizzes
 ┃ ┣ 📜 Lecture_1.srt            <-- Subtitles file
 ┃ ┣ 📜 CheatSheet.pdf           <-- Attached resources for this lesson
 ┃ ┗ 📜 Lecture_2.aashiq
 ┗ 📂 Chapter_2_Advanced
   ┣ 📜 Lesson_3.aashiq
   ┗ 📜 Lesson_3.md
```

### ❗️ IMP: Videos Srf Aashik App me kaise chalaye? (How to secure videos?)
अगर आप चाहते हैं कि आपके वीडियोस गैलरी, MX Player या VLC में न चलें और केवल **AASHIQ+ App** में चलें:
1. वीडियो को कभी भी `.mp4` या `.mkv` रूप में सीधे इम्पोर्ट न करें।
2. वीडियो को **XOR Encrypt** करें जिससे उसका बाइनरी डेटा सेक्योर हो जाए और फ़ाइल एक्सटेंशन `.aashiq` करदें।
3. **सिर्फ रीनेम करदेने से वीडियो नहीं चलेगा!** उसको पहले नीचे दिए गए स्क्रिप्ट से एनक्रिप्ट (XOR) करना आवश्यक है।

---

## 💻 PART 2: Python Script for Video Encryption (वीडियो एनक्रिप्शन का तरीका)

आप अपने PC/Mac/Linux में Python की मदद से किसी भी `.mp4` वीडियो को `.aashiq` में बदल सकते हैं:

1. अपने कंप्यूटर में Python इनस्टॉल करें।
2. नीचे दिए गए कोड को `encrypt_course.py` नाम से सेव करें।
3. **कमांड प्रॉम्प्ट (CMD / Terminal) कहाँ खोलें?**
   - अपने कंप्यूटर में उस फ़ोल्डर में जाएं जहाँ आपकी `.mp4` वीडियो और `encrypt_course.py` दोनों रखी हैं।
   - फ़ोल्डर के एड्रेस बार में `cmd` लिखकर Enter दबाएँ या टर्मिनल खोलकर उस फ़ोल्डर तक `cd path/to/folder` से पहुँचें।
4. निम्नलिखित कमांड चलाएं:
   ```bash
   python encrypt_course.py my_lecture.mp4
   ```
5. यह कमांड `my_lecture.aashiq` फ़ाइल बना देगा। इसे अपने ऑफलाइन कोर्स फ़ोल्डर में डालें।

### 📜 python encrypt_course.py Code:
```python
import sys
import os

def encrypt_decrypt_file(filepath, key=0xAE):
    # Symmetric XOR encryption
    if not os.path.exists(filepath):
        print(f"Error: File '{filepath}' not found!")
        return
    
    base, _ = os.path.splitext(filepath)
    output_path = base + ".aashiq"
    
    print(f"Securing: {filepath} -> {output_path}")
    
    with open(filepath, 'rb') as f_in:
        data = bytearray(f_in.read())
        
    # Byte-by-byte symmetric XOR
    for i in range(len(data)):
        data[i] ^= key
        
    with open(output_path, 'wb') as f_out:
        f_out.write(data)
        
    print("Success! Your video is safely encrypted for AASHIQ+.")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python encrypt_course.py <video_file.mp4>")
    else:
        encrypt_decrypt_file(sys.argv[1])
```

---

## 📝 PART 3: .md Notes Taiyar Kaise Kare? (How to Write .md Notes)

AASHIQ+ का Advanced Notes Parser न केवल साधारण टेक्स्ट बल्कि इमेज, क्विज़, दो-कॉलम लेआउट, फ़्लैशकार्ड, टेबल्स और कस्टमाइज़्ड इन्फो ब्लॉक्स रेंडर कर सकता है। नीचे दिए गए टेम्पलेट को देखकर आप अपनी `Lecture_1.md` फ़ाइल तैयार करें:

### 🌟 Copy & Use this Markdown Template:
```markdown
# 🚀 1. Learn Jetpack Compose (Header H1)
यहाँ से आपका प्रीमियम लेक्चर नोट्स शुरू होता है। आप सामान्य पैराग्राफ लिख सकते हैं।

## 📊 Comparison Box (तुलना ब्लॉक)
:::comparison [BEFORE] | [AFTER]
XML Layouts are verbose and hard to maintain | Jetpack Compose utilizes reactive Kotlin code and simplifies layout systems
Custom views took 200 lines of XML | Custom layouts take 15 lines of Compose code
:::

## 📝 Checklists (चेकलिस्ट)
- [ ] Install Android Studio Ladybug
- [ ] Master Kotlin StateFlow & Coroutines
- [x] Integrate Media3 ExoPlayer

## 📋 Markdown Tables (सारणी/तालिका)
| Functionality | Old Android | AASHIQ+ Platform |
| :--- | :--- | :--- |
| Video Player | Traditional VideoView | Premium Media3 Player |
| Local Notes | Strings Only | Multitasking MD + HTML |
| Certification | Manual PDF | Automated Gold Credential |

## 💡 Info/Warning Panels (सूचना बॉक्स)
:::info
हमेशा ExoPlayer का लाइफसाइकल कंट्रोल ध्यान से संभालें ताकि मेमोरी लीक न हो।
:::

:::warning
बिना XOR एनक्रिप्शन के .aashiq नाम रखने पर वीडियो गैलरी में करप्ट रेंडर होगा। पहले Python टूल ज़रूर इस्तेमाल करें!
:::

## ❓ Dynamic Inline Quiz (क्विज़ ब्लॉक)
:::quiz
Question: What is the XOR encryption key used by AASHIQ+ to decode videos?
A) 0xAA
B) 0xFF
C) 0xAE
D) 0x12
Answer: C
Explanation: The app uses 0xAE as the default symmetric key to decrypt byte streams on-the-fly.
:::

## 🎴 Flashcard Components (फ़्लैशकार्ड)
:::flashcard
Front: What is MVVM?
Back: Model-View-ViewModel is an architectural pattern that separates the UI (View) from the business logic (ViewModel) using data binding and states.
:::

## 🖼️ Embed Gallery & Images (इमेज इम्पोर्ट)
आप अपनी लोकल इमेज या इन्टरनेट इमेज इस प्रकार जोड़ सकते हैं:
![Premium Architecture Graph](images/architecture_graph.png)

## 🏛️ Two Columns Grid Layout (कॉलम लेआउट)
:::columns
[Left Block]
यह बाईं ओर (Left Column) की जानकारी है। यहाँ पर आप बेसिक कॉन्सेप्ट्स लिख सकते हैं।
[Right Block]
यह दाईं ओर (Right Column) की जानकारी है। यहाँ आप कस्टमाइज्ड कोड स्निपेट्स और रिफरेन्स जोड़ सकते हैं।
:::

## 📎 Embedded PDF & Resource Links
अगर आप नोट्स के बीच में कोई पीडीएफ डाउनलोड करने का लिंक या अटैचमेंट जोड़ना चाहते हैं:
:::resource
Type: PDF
Title: Lecture Slides: Jetpack Compose Deep Dive
Uri: file:///android_asset/ComposeSlides.pdf
:::
```

---
💡 **AASHIQ+ App Tip**: जब आप कोई भी फ़ोल्डर SAF (Storage Access Framework) की मदद से इम्पोर्ट करते हैं, ऐप इन नोट्स और सबटाइटल्स को वीडियो नेम से ऑटो-मैप करके आपके स्क्रीन पर डिस्प्ले कर देता है। Enjoy Premium Learning!
