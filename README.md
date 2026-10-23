# Ko'z va bo'yin mashqlari (Android)

Ekran vaqtini hisoblaydi va belgilangan vaqtdan keyin ko'z/bo'yin mashqlari uchun eslatma yuboradi.

## APK ni GitHub'da yig'ish
1. github.com da yangi repository oching (masalan `koz-boyin-mashq`).
2. Shu papkadagi BARCHA fayllarni (yashirin `.github` papkasi bilan) repository'ga yuklang.
3. Repository'da **Actions** bo'limiga kiring, "Build APK" ishga tushganini kuting (5-10 daqiqa).
4. Tugagach, ish ichidagi **Artifacts** bo'limidan `koz-boyin-mashq-apk` ni yuklab oling, zip ichida `app-debug.apk` bor.
5. APK ni telefonga o'tkazib o'rnating (Sozlamalarda "noma'lum manbalardan o'rnatish" ruxsati kerak bo'lishi mumkin).

## Birinchi ishga tushirish
- "Foydalanish statistikasiga kirish" ruxsatini bering (ilova ro'yxatidan shu ilovani yoqing).
- Android 13+ da bildirishnomaga ruxsat bering.
- Xiaomi, Samsung, Huawei: ilova uchun "Avto-ishga tushirish" ni yoqing va batareya cheklovini "Cheklovsiz" qiling.

## Eslatma
- Fon tekshiruvi har ~15 daqiqada ishlaydi, shuning uchun eslatma belgilangan vaqtdan 15 daqiqagacha kechikishi mumkin.
- Minimal Android versiyasi: 8.0 (API 26).
