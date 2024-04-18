package tg.ibcp;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

public class NetworkUtils {
    public static void bindProcessToWiFiNetwork(ConnectivityManager cm){
        for (Network n: cm.getAllNetworks()){
            NetworkCapabilities nc = cm.getNetworkCapabilities(n);
            if(nc!=null && nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)){
                cm.bindProcessToNetwork(n);
                break;
            }
        }
    }
}
