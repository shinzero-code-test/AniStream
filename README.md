# AniStream

`AniStream` هو تطبيق أندرويد مستقل مبني بالكامل بـ Kotlin وJetpack Compose لمشاهدة الأنمي عبر الكشط المحلي المباشر من `Anime3rb.com` بدون أي خادم وسيط.

يعتمد التطبيق على:

- `MVVM + Clean Architecture`
- `Jetpack Compose`
- `OkHttp + Jsoup`
- `Room + DataStore`
- `Media3 / ExoPlayer`
- `Duktape` عند الحاجة لفك الروابط أو التعامل مع التضمينات

## الفكرة الأساسية

التطبيق لا يقرأ بيانات البث من واجهة برمجية خارجية، ولا يستخدم Backend خاصاً به.

بدلاً من ذلك:

- يجلب الصفحات مباشرة من `Anime3rb.com`
- يحلل البنية الحقيقية للصفحات عبر `Jsoup`
- يستخدم ملفات HTML المحلية الموجودة لديك فقط كمرجع للتحقق من المحددات الصحيحة والبنية العامة قبل كتابة منطق الكشط

هذا يعني أن التطبيق يظل أقرب ما يمكن لسلوك الموقع الحقيقي، مع الحفاظ على التنفيذ داخل الجهاز فقط.

## الميزات الحالية

### 1) الصفحة الرئيسية

- قسم الأنميات المثبتة
- قسم أحدث الحلقات
- قسم آخر الأنميات المضافة
- قسم أكمل المشاهدة اعتماداً على السجل المحلي
- فهرس مباشر من Anime3rb مع تحميل تدريجي للصفحات
- ترتيب مباشر للفهرس والبحث حسب:
  - تاريخ الإضافة
  - الاسم
  - تاريخ الإصدار
  - التقييم

### 2) البحث

- البحث المباشر في Anime3rb.com
- عرض نتائج تفصيلية
- تحميل المزيد من نتائج البحث

### 3) صفحة العمل

- البوستر والعنوان والنوع
- الحالة والإصدار والاستديو والمؤلف والتصنيف العمري
- القصة الكاملة أو الملخص
- الأسماء الأخرى
- قائمة الحلقات
- العروض التشويقية
- الروابط الخارجية للمصادر
- الأنميات المشابهة المقترحة
- إدارة الحالة المحلية للمشاهدة
- التقييم الشخصي المحلي

### 4) شاشة المشغل

- تشغيل عبر `Media3 / ExoPlayer`
- دعم `HLS .m3u8` و `MP4` عند توفرهما
- اختيار المصدر المتاح
- عرض عدد المشاهدات
- عرض روابط التحميل المباشر
- رابط تحميل جميع الحلقات عندما يتوفر
- الانتقال للحلقة التالية يدوياً أو تلقائياً حسب الإعدادات

### 5) المكتبة المحلية

- شاشة قائمة مخصصة
- شاشة سجل مخصصة
- تصنيف الأعمال حسب حالة المشاهدة
- استكمال الحلقات من آخر موضع محفوظ

### 6) الإعدادات

- تشغيل الحلقة التالية تلقائياً
- تفضيل عرض الملخص
- حفظ الوضع السينمائي
- تفعيل/تعطيل الألوان الديناميكية
- مسح السجل
- مسح القائمة

## البنية المعمارية

المشروع منظم إلى طبقات واضحة:

### `presentation/`

واجهات Compose + ViewModels + Navigation

- `dashboard/`
- `details/`
- `player/`
- `library/`
- `settings/`
- `navigation/`
- `root/`

### `domain/`

نماذج الأعمال وحالات الاستخدام والمستودعات المجردة

- `model/`
- `repository/`
- `usecase/`

### `data/`

مصادر البيانات الفعلية

- `scraper/` محرك الكشط
- `repository/` تنفيذ المستودعات
- `local/` قاعدة البيانات
- `preferences/` إعدادات DataStore

### `core/`

البنية المشتركة

- `network/`
- `webview/`
- `common/`

## منطق الكشط

يتم استخدام محددات موثقة من ملفات HTML المحلية التي زودتني بها، مثل:

- `home.html`
  - `a.video-card`
  - `#videos a.video-card`
  - `div.title-card`

- `animelist.html`
  - `div.titles-list div.title-card`
  - `a.details`
  - `div.genres span`
  - `p.synopsis`

- `animedetails.html`
  - `h1.text-2xl`
  - `table.leading-loose`
  - `div[x-data*=summary]`
  - `div#trailers iframe[data-src]`
  - `div.video-list a[href*=/episode/]`
  - قسم `أنميات مشابهة مقترحة`

- `animeepisode.html`
  - `iframe`
  - `button[data-video-source]`
  - `a[href*=/download/]`
  - قيم `video_url`, `video_source`, `views` من `wire:snapshot`

## Cloudflare وملفات الكوكيز

عند مواجهة تحدي Cloudflare:

- يتم تشغيل `WebView` غير مرئي في الخلفية عبر `CloudflareWebViewInterceptor`
- يتم تحميل الصفحة بنفس ترويسات المتصفح الفعلية
- يتم استخراج الكوكيز مثل `cf_clearance` وUser-Agent الفعلي من الجلسة
- يتم تمريرها إلى عميل `OkHttp` وطلبات `Jsoup` اللاحقة عبر `WebSessionStore`

الملفات الأساسية:

- `app/src/main/java/com/exapps/anistream/core/network/CloudflareChallengeInterceptor.kt`
- `app/src/main/java/com/exapps/anistream/core/webview/CloudflareWebViewInterceptor.kt`
- `app/src/main/java/com/exapps/anistream/core/network/WebSessionStore.kt`

## البيانات المحلية

### Room

يتم حفظ:

- القائمة المحلية مع الحالة والتقييم الشخصي
- السجل وآخر موضع تشغيل

### DataStore

يتم حفظ:

- التشغيل التلقائي للحلقة التالية
- تفضيل الملخص
- الوضع السينمائي
- الألوان الديناميكية

## التوطين العربي

التطبيق الآن مهيأ بواجهة عربية واتجاه RTL، مع اعتماد النصوص العربية في عناصر التنقل والشاشات والإعدادات والرسائل الأساسية.

## البناء محلياً

### المتطلبات

- Android SDK
- JDK 17
- Gradle Wrapper

### أمر البناء

```bash
./gradlew assembleDebug
```

بعد التفعيل الحالي لتقسيم المعماريات، سيتم إنشاء APKs متعددة لكل ABI بالإضافة إلى نسخة `universal`.

## GitHub Actions و Releases

تم تجهيز سير عمل أوتوماتيكي في:

`/.github/workflows/android-apk.yml`

وظيفته:

- بناء APKs لكل المعماريات
- رفعها كـ Artifacts
- إنشاء GitHub Release تلقائياً على `main`
- إرفاق ملفات الـ APK
- استخدام القسم المطابق من `CHANGELOG.md`

## إدارة الإصدارات

الإصدار الحالي موجود في:

- `gradle.properties`

المفاتيح:

- `APP_VERSION_CODE`
- `APP_VERSION_NAME`

## الملفات المهمة

- `app/build.gradle.kts`
- `app/src/main/java/com/exapps/anistream/data/scraper/Anime3rbExtractor.kt`
- `app/src/main/java/com/exapps/anistream/data/scraper/Anime3rbHtmlParser.kt`
- `app/src/main/java/com/exapps/anistream/core/webview/CloudflareWebViewInterceptor.kt`
- `app/src/main/java/com/exapps/anistream/presentation/navigation/AniStreamNavGraph.kt`
- `app/src/main/java/com/exapps/anistream/presentation/library/LibraryScreen.kt`
- `app/src/main/java/com/exapps/anistream/presentation/settings/SettingsScreen.kt`
- `CHANGELOG.md`

## ملاحظات مهمة

- التطبيق يعتمد على البنية الحالية للموقع، لذلك أي تغيير كبير في HTML الخاص بـ Anime3rb قد يتطلب تحديث المحددات.
- ملفات HTML المحلية ليست مصدر البيانات داخل التطبيق، بل مرجع تحقق للمحددات فقط.
- بعض المزايا المرتبطة بالحسابات أو الدفع داخل الموقع لا تعتمد على Backend داخل التطبيق، لأن المشروع مصمم ليبقى محلياً بالكامل.

## الترخيص

هذا المستودع مخصص لأغراض التطوير والتجربة المعمارية والتعليمية ضمن سياق المشروع الحالي.
