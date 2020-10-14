
# react-native-android-easy-updates

## Getting started

`$ npm install react-native-android-easy-updates --save`

### Mostly automatic installation

`$ react-native link react-native-android-easy-updates`

### Manual installation

#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import com.androideasyupdates.RNAndroidEasyUpdatesPackage;` to the imports at the top of the file
  - Add `new RNAndroidEasyUpdatesPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-android-easy-updates'
  	project(':react-native-android-easy-updates').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-android-easy-updates/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-android-easy-updates')
  	```

## Usage
```javascript
import RNAndroidEasyUpdates from 'react-native-android-easy-updates';
```
 ##API
 
 All the APIs return promise. Please go to 
 https://developer.android.com/guide/playcore/in-app-updates
 for more information about different function and parameter information.
 
 ####checkUpdateAvailability()
 Checks and starts app update if available for particular updateType
 1. Input Params:
     - updateType - IMMEDIAT/FELXIBLE
 
 ####Example
 ```javascript
 RNAndroidEasyUpdates.checkUpdateAvailability(updateType).then((status) => {
     //update success status
 });
 ```
 ####completeUpdate()
 Starts complete app update if available
 
 ####Example
 ```javascript
 RNAndroidEasyUpdates.completeUpdate().then((status) => {
     //update success status
 });
 ```