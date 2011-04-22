package src.com.elsdoerfer.android.autostarts.opt;

import com.elsdoerfer.android.autostarts.R;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/**
 * Market-specific functionality.
 */
public class MarketUtils {

	public static final int FIND_IN_MARKET_TEXT = R.string.find_in_market;

	public static void findPackageInMarket(Context ctx, String packageName) {
		try {
			Intent marketIntent = new Intent(Intent.ACTION_VIEW);
			marketIntent.setData(Uri.parse("market://search?q=pname:"+packageName));
			ctx.startActivity(marketIntent);
		}
		catch (ActivityNotFoundException e) {}
	}

}
