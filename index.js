
import { NativeModules } from 'react-native';

const { RNAndroidEasyUpdates } = NativeModules;

class AndroidEasyUpdates {

    static checkUpdateAvailability(updateType) {
        return RNAndroidEasyUpdates.checkUpdateAvailability(updateType);
    }

    static completeUpdate() {
        return RNAndroidEasyUpdates.completeUpdate();
    }
}

export default AndroidEasyUpdates;
