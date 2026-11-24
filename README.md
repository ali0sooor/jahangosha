# جهانگشا — نسخه اندروید

بازی داخل یک WebView اجرا می‌شود و همه‌چیز (فونت‌ها و خود بازی) داخل برنامه است، پس بدون اینترنت هم کار می‌کند.

## دو نسخه (Build Variant)
- `normal` — نسخه عادی، نام برنامه «جهانگشا»
- `cheat` — نسخه تستی با طلا و نیروی انسانی بی‌نهایت، نام «جهانگشا (تست)». شناسه‌اش جداست و کنار نسخه عادی نصب می‌شود.

## ساخت APK با Android Studio
1. پوشه `JahangoshaAndroid` را در Android Studio باز کنید و صبر کنید Gradle همگام شود.
2. از منوی Build Variants یکی از `normalDebug` یا `cheatDebug` را انتخاب کنید.
3. Build ← Build APK(s).
4. فایل‌ها اینجا ساخته می‌شوند:
   - `app/build/outputs/apk/normal/debug/app-normal-debug.apk`
   - `app/build/outputs/apk/cheat/debug/app-cheat-debug.apk`

## ساخت APK بدون Android Studio (GitHub Actions)
پروژه را در یک مخزن GitHub بگذارید؛ در تب Actions گزینه «Build APK» را اجرا کنید و APKها را از Artifacts بردارید.

## تبلیغات
- بازی منطق تبلیغ پاداشی را دارد و برنامه آن را با AdMob وصل می‌کند.
- الان شناسه‌های **آزمایشی** گوگل گذاشته شده‌اند. قبل از انتشار این دو را عوض کنید:
  - `app/src/main/AndroidManifest.xml` ← مقدار `com.google.android.gms.ads.APPLICATION_ID`
  - `MainActivity.java` ← ثابت `REWARDED_AD_UNIT`

## انتشار در گوگل‌پلی
- از Build ← Generate Signed Bundle / APK با کلید امضای خودتان خروجی release (AAB) بگیرید.
- `applicationId` را در `app/build.gradle` با شناسه دلخواه خودتان عوض کنید.
- نسخه `cheat` را منتشر نکنید.

## نکته‌ها
- دکمه بازگشت: ابتدا پنجره باز را می‌بندد، بعد منوی بازی را باز می‌کند و در صفحه شروع از برنامه خارج می‌شود.
- «خروجی فایل» در منوی ذخیره‌ها، پنجره انتخاب محل ذخیره اندروید را باز می‌کند و «ورود از فایل» انتخاب‌گر فایل را.
- بازی هنگام رفتن به پس‌زمینه خودکار ذخیره می‌شود.
- حداقل نسخه اندروید ۸٫۰ (API 26).
