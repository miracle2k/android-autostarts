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

public class HelpActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help);

        // Build the FAQ text.
        // TODO: The string-array items use HTML formatting, which
        // is lost here. There doesn't seem to be a way to read the
        // string-array resource as a Spannable?
        CharSequence[] faq = getResources().getTextArray(R.array.faq);
        StringBuilder fullText = new StringBuilder("<html><body>");
        boolean isQuestion = false;
        for (int i=0; i<faq.length; i++) {
        	// The array contains alternating questions and answers
        	isQuestion = !isQuestion;

        	String entry;
        	if (faq[i] instanceof Spanned)
        		entry = Html.toHtml((Spanned)faq[i]);
        	else {
	        	if (isQuestion)
	        		entry = "<p><b>" + faq[i].toString() + "</b></p>";
	        	else {
	        		entry = "<p>" + faq[i].toString() + "</p>";
	        	}
        	}
        	fullText.append(entry);
        }
        fullText.append("</body></html>");

        Log.d("sdf", fullText.toString());

        ((WebView)findViewById(R.id.faq_text)).loadData(
        		fullText.toString(), "text/html", "utf-8");

        ((Button)findViewById(R.id.close)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finish();
			}
        });
    }
}
