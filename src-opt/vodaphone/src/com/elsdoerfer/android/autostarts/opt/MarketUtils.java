package src.com.elsdoerfer.android.autostarts.opt;

import com.elsdoerfer.android.autostarts.R;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.lang.UnsupportedOperationException;

/**
 * Vodaphone App-Select-specific functionality.
 */
public class MarketUtils {

	// We don't really know how to link to other apps in AppSelect.
	// 0 will disable the menu item.
	public static final int FIND_IN_MARKET_TEXT = 0;

	public static void findPackageInMarket(Context ctx, String packageName) {
		throw new UnsupportedOperationException();
	}

}