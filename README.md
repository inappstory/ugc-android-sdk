# UGC Editor

A library with UGC Editor that works with [InAppStory library](https://github.com/inappstory/android-sdk/blob/main/README.md#getting-started). 

## Requirements

The minimum SDK version is 21 (Android 5.0).

Version's compapibility

| UGC SDK version                  | InAppStory SDK version 		|
|----------------------------------|------------------------------------|
|   1.1.0                          | 1.15.0-rc1                        	|
|   1.0.7                          | 1.12.4                         	|
|   1.0.6                          | 1.12.0-rc7                         |
|   1.0.5                          | 1.12.0-rc3+                        |
|   1.0.4 	                   | 1.12.0-rc2                         |
|   1.0.0 - 1.0.2                  | 1.9.1 - 1.11.1                     |


The library is intended for Phone and Tablet projects (not intended for Android TV or Android Wear applications).

## Adding to the project

Add jitpack maven repo to the root `build.gradle` in the `repositories` section :
```gradle
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```

In the project `build.gradle` (app level) in the `dependencies` section add dependency to InAppStory library:
```gradle
implementation("com.github.inappstory:android-sdk:$inappstory_version") {
	transitive=true
}
```

And then add dependency to UGC library (Latest release version is 1.1.0):

```gradle
implementation("com.github.inappstory:ugc-android-sdk:$ugc_version") {
	transitive=true
	exclude group: 'com.github.inappstory', module: 'android-sdk' //exclude to prevent libraries overriding
}
```

## ProGuard

You can use same Proguard rules as [InAppStory Proguard rules](https://github.com/inappstory/android-sdk#proguard):


## Initialization and Editor Usage

1) [Initialize InAppStory SDK](https://github.com/inappstory/android-sdk#sdk-initialization)
2) In [AppearanceManager](https://github.com/inappstory/android-sdk#sdk-initialization) (in global or for `StoriesList`) you need to set `csHasUgc` as true.
3) For `StoriesList` set click callback from UGC item with `setOnUGCItemClick(callback: OnUGCItemClick)`. In callback you can open Editor with method from `UGCInAppStoryManager.openEditor(ctx: Context, ugcInitData: HashMap<String, Any?>? = null)`

For example:

```js 
	appearanceManager.csHasUgc(true); 
	storiesList.setAppearanceManager(appearanceManager)
	storiesList.setOnUGCItemClick {
		UGCInAppStoryManager.openEditor(context)
  	}
  	storiesList.loadStories()
```
If you want to force close editor, you can use method `UGCInAppStoryManager.closeEditor(closeCallback: () -> Unit)` //closeCallback has empty body by default
For example:

```js 
	UGCInAppStoryManager.closeEditor { 
		Log.e(TAG, "Close editor") 
	}
```

## Customization

UGC item in `StoriesList` can be customized. To do this you need to set `csListUGCItemInterface` in `AppearanceManager`.

```js
public interface IStoriesListUGCItem {
    	View getView(); // here you need to pass View - the appearance of the cell
}
```

## UGC stories lists
[Here you can read full guide, how to implement it](https://github.com/inappstory/android-sdk/blob/ugc_merge/docs/UgcStoriesList.md)
