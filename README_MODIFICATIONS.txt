Добавлено: кнопка скрытия ярлыков только для выбранного приложения и расширенный список заранее поддержанных приложений из liapp.store/page2. Сопоставление новых приложений сделано по названию выбранного приложения (а где известно — и по package name).

2026-04-02 changes:
- Launcher labels shortened to English format: <shortname> <screenNumber>.
- Screen numbers: 1=main, 2=passenger, 3=full, 4=ceiling.
- Added verified package-name matching for a subset of public apps; title matching remains as fallback for LiApp-specific variants.


Update 2026-04-02 (popular head-unit apps added)
- Added new supported apps app109-app123 for common Android head-unit use cases in Russia.
- Verified package names where public primary sources exposed them.
- Added title-only fallback entries for FCC Car Launcher, Crony Auto, and NavRadio+ when package IDs could not be confirmed from primary public app sources.
- Kept the working launcher-component model unchanged.

Update 2026-04-06
- Быстрые кнопки запуска перенесены в строки списка приложений.
- Добавлен режим закрепления через alias-activity: долгий тап создаёт один ярлык под выбранный экран.
- Повторный долгий тап снимает закрепление.
- Добавлена вкладка «Отладка» с диагностикой и инженерными инструментами.
- Добавлен блок APK загрузки/установки из публичной папки Яндекс.Диска.
- Долгое нажатие на кнопку обновления в шапке Отладки перезагружает список APK.
