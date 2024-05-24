package codex.tmwott.utilities;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

import io.github.edsuns.adfilter.AdFilter;
import io.github.edsuns.adfilter.Filter;
import io.github.edsuns.adfilter.FilterViewModel;

public class AdBlockUtilities {
    private static AdFilter filter;
    private static FilterViewModel filterViewModel;

    public static void init(Context context){
        filter = AdFilter.Companion.create(context);
        filterViewModel = filter.getViewModel();
        downloadFilter();
    }

    public static void downloadFilter(){
        Map<String, String> map = new HashMap<>();
        map.put("AdGuard Base", "https://filters.adtidy.org/extension/chromium/filters/2.txt");
        map.put("EasyPrivacy Lite", "https://filters.adtidy.org/extension/chromium/filters/118_optimized.txt");
        map.put("AdGuard Tracking Protection", "https://filters.adtidy.org/extension/chromium/filters/3.txt");
        map.put("AdGuard Annoyances", "https://filters.adtidy.org/extension/chromium/filters/14.txt");
        map.put("AdGuard Chinese", "https://filters.adtidy.org/extension/chromium/filters/224.txt");
        map.put("NoCoin Filter List", "https://filters.adtidy.org/extension/chromium/filters/242.txt");

        for (Map.Entry<String, String> entry : map.entrySet()) {
            Filter subscription = filterViewModel.addFilter(entry.getKey(), entry.getValue());
            filterViewModel.download(subscription.getId());
        }
    }

    public static AdFilter getFilter() {
        return filter;
    }

    public static FilterViewModel getFilterViewModel() {
        return filterViewModel;
    }

    public static boolean getHasInstallation(){
        return filter.getHasInstallation();
    }

}
