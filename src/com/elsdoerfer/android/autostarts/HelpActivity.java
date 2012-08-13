package com.elsdoerfer.android.autostarts;

import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.Button;
import src.com.elsdoerfer.android.autostarts.opt.RootFeatures;


public class HelpActivity extends Activity {

	private static int[] DefaultFaq = {
		R.array.faq1,
		R.array.faq2,
		R.array.faq3,
		R.array.faq4,
		R.array.faq5,
		R.array.faq6,
	};

	// Does not include questions about root features
	private static int[] NoRootFaq = {
		R.array.faq1,
		R.array.faq3,
		R.array.faq4
	};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help);

        // Build the FAQ text.
	    int[] faqConfig = RootFeatures.Enabled ? DefaultFaq : NoRootFaq;

        StringBuilder fullText = new StringBuilder("<html><body>");
        boolean isQuestion = false;
        for (int i=0; i<faqConfig.length; i++) {
	        // TODO: The string-array items use HTML formatting, which
	        // is lost here. There doesn't seem to be a way to read the
	        // string-array resource as a Spannable?
	        CharSequence[] question = getResources().getTextArray(faqConfig[i]);

	        for (int j=0; j<=1; j++) {
	            // The array contains alternating questions and answers
	            isQuestion = !isQuestion;

	            String entry;
	            if (question[j] instanceof Spanned)
	                entry = Html.toHtml((Spanned)question[j]);
	            else {
		            if (isQuestion)
		                entry = "<p><b>" + question[j].toString() + "</b></p>";
		            else {
		                entry = "<p>" + question[j].toString() + "</p>";
		            }
	            }
	            fullText.append(entry);
	        }
        }
        fullText.append("</body></html>");

        ((WebView)findViewById(R.id.faq_text)).loadData(
        		fullText.toString(), "text/html", "utf-8");

        ((Button)findViewById(R.id.close)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finish();
			}
        });
    }
}
