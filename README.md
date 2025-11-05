## Original source locations

> All changes are based on Android 15 (android-15.0.0_r23)

### app

- `CarActivityService`: 'packages/services/Car/service'

### shell-ext

- `system`: 'packages/apps/Car/SystemUI'
- `view` and `util` classes: 'packages/services/Car/car-builtin-lib'
- `aidl` and others: 'packages/services/Car/car-lib'
- `CarActivityServiceProvider`: created to host partial logics of `Car`

> A few irrelative codes are removed.

## Basic architecture

### In AAOS

- `CarSystemUI`: a proxy of `WindowManagerService`, manage tasks and handle transitions
- `CarService`: a channel between `CarSystemUI` and APP, provide more user-friendly interfaces
- APP(`car-lib`): host `CarRemoteTaskView`

## How to integrate into AOSP

1. Add shell-ext to `wm-shell`
   - Copy `shell-ext/src/main/aidl` to `frameworks/base/libs/WindowManager/Shell/src`
   - Copy `shell-ext/src/main/java` to `frameworks/base/libs/WindowManager/Shell/src`
2. Integrate shell-ext to `SystemUI`
   - Apply `files/SystemUI.patch` to `frameworks/base`
3. Build app into AOSP
   - Add folder in AOSP, e.g. `packages/apps/TaskViewMod`
   - Copy `app/src` into `packages/apps/TaskViewMod`
   - Copy `files/Android.bp` into `packages/apps/TaskViewMod`
   - Copy `files/privapp-io.github.ole.taskview-whitelist.xml` into `packages/apps/TaskViewMod`
   - Add `TaskViewMod` to `PRODUCT_PACKAGES` in the device make file
4. APP gets `CarActivityManager` via `CarActivityServiceProvider`
    - Use `TaskViewController` to create `RemoteCarTaskView` and add it to your layout
