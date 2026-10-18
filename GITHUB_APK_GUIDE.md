# راهنمای ساخت APK با GitHub (بدون Android Studio)

## مرحله ۱: ساخت حساب GitHub
اگر نداری: https://github.com/signup

## مرحله ۲: ساخت Repository جدید
1. برو به https://github.com/new
2. نام بگذار مثلاً: `electrical-troubleshoot-android`
3. Public انتخاب کن (برای رایگان بودن Actions)
4. Create repository بزن

## مرحله ۳: آپلود پروژه

### روش آسان (از سایت GitHub):
1. داخل ریپوی خالی، روی **uploading an existing file** بزن
2. همه فایل‌های داخل پوشه `electrical-android` را بکش و رها کن
3. Commit changes بزن

### روش با Git (اگر Git داری):
```bash
cd electrical-android
git init
git add .
git commit -m "First commit - Android app"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/electrical-troubleshoot-android.git
git push -u origin main
```

## مرحله ۴: ساخت خودکار APK
1. بعد از Push، برو به تب **Actions** در همان ریپو
2. workflow به نام **Build Debug APK** را می‌بینی
3. صبر کن تا سبز شود (حدود ۵–۱۰ دقیقه)
4. روی همان run کلیک کن
5. پایین صفحه بخش **Artifacts** → فایل `electrical-troubleshoot-debug` را دانلود کن
6. فایل را از حالت zip خارج کن → `app-debug.apk`

## مرحله ۵: نصب روی گوشی
1. فایل APK را به گوشی منتقل کن (واتساپ، کابل، گوگل درایو)
2. در تنظیمات گوشی: نصب از منابع ناشناس را برای File Manager فعال کن
3. روی APK بزن و Install کن

## تنظیم آدرس سرور
APK دیباگ آدرس پیش‌فرض دارد:
`http://10.0.2.2:5000/api/` (فقط برای شبیه‌ساز)

برای گوشی واقعی باید قبل از Build، فایل زیر را ویرایش کنی:
`app/src/main/java/com/electrical/troubleshoot/util/Constants.kt`

و IP سیستم خودت را بگذاری، مثلاً:
```
const val BASE_URL = "http://192.168.1.5:5000/api/"
```

سپس دوباره commit و push کن تا APK جدید ساخته شود.

## نکته مهم
بک‌اند باید روی همان شبکه (یا اینترنت با IP عمومی) در حال اجرا باشد تا اپ به آن وصل شود.
