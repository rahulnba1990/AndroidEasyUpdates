
import { NativeModules } from 'react-native';

const { RNAndroidEasyUpdates } = NativeModules;

class AndroidEasyUpdates {

    static checkUpdateAvailability(stalenessDays) {
        return RNAndroidEasyUpdates.checkUpdateAvailability(stalenessDays);
    }
}

export default AndroidEasyUpdates;
