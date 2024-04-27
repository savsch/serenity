package tg.ibcp;

import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;

public class NetworkUtils {
    public static LinkProperties bindProcessToWiFiNetwork(ConnectivityManager cm){
        LinkProperties lp = null;
        for (Network n: cm.getAllNetworks()){
            NetworkCapabilities nc = cm.getNetworkCapabilities(n);
            if(nc!=null && nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)){
                cm.bindProcessToNetwork(n);
                lp = cm.getLinkProperties(n);
                break;
            }
        }
        return lp;
    }
}
