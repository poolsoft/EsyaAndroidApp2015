package com.iiitd.esya.app;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.florent37.materialviewpager.MaterialViewPagerHelper;
import com.github.florent37.materialviewpager.adapter.RecyclerViewMaterialAdapter;

import org.xml.sax.XMLReader;


/**
 * A placeholder fragment containing a simple view.
 */

/**
 * The cards are hardcoded as of now, we have to use RecyclerView othervise this wont work. The toolbar will not collapse while using ListView.
 * For recycleView Reference:  http://enoent.fr/blog/2015/01/18/recyclerview-basics/
 */
public class EventDetailFragment extends Fragment {


    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;

    private TestRecyclerViewAdapter changeableAdapter;


    private static final int ITEM_COUNT = 1;

    public String mDescription;

    public static EventDetailFragment newInstance(String details)
    {
        EventDetailFragment temp = new EventDetailFragment();
        temp.mDescription = details;
        return temp;
    }

    public void updateDescription(String details_)
    {
        mDescription = details_;
        changeableAdapter.updateDescription(details_);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);

        changeableAdapter = new TestRecyclerViewAdapter(mDescription);
        mAdapter = new RecyclerViewMaterialAdapter(changeableAdapter);
        mRecyclerView.setAdapter(mAdapter);

        MaterialViewPagerHelper.registerRecyclerView(getActivity(), mRecyclerView, null);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if(isVisibleToUser){
            if (mRecyclerView != null)
            {
                mRecyclerView.smoothScrollToPosition(0);
                MaterialViewPagerHelper.getAnimator(getActivity()).onMaterialScrolled(null, 0);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

//        View view = inflater.inflate(R.layout.fragment_event_detail, container, false);
//        ((TextView)view.findViewById(R.id.text_event_details)).setText(mDescription);
//        return view;
        return inflater.inflate(R.layout.fragment_event_detail, container, false);
    }
}

class TestRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
{

    private String details;
    private TextView detailsTextView;



    public TestRecyclerViewAdapter(String details) {
        this.details = details;
    }

    // remove trailing whitespace in HTML without removing formatting
    // http://stackoverflow.com/a/10187511/2851353
    private static CharSequence trimTrailingWhitespace(CharSequence source) {
        if(source == null) return "";
        int i = source.length();
        // loop back to the first non-whitespace character
        while(--i >= 0 && Character.isWhitespace(source.charAt(i))) {}
        return source.subSequence(0, i+1);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_card_big, parent, false);
        TextView textView = (TextView)view.findViewById(R.id.event_details_text);
        detailsTextView = textView;

        updateDescription(details);

        return new RecyclerView.ViewHolder(view){};
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
    }

    public void updateDescription(String details_)
    {
        this.details = details_;
        if (this.details == null || this.details.trim().isEmpty())
        {
            detailsTextView.setText("");
            return;
        }
        detailsTextView.setText(trimTrailingWhitespace(Html.fromHtml(details, null, new Html.TagHandler() {
            boolean first= true;
            String parent=null;
            int index=1;
            @Override
            public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
                if(tag.equals("ul")) parent="ul";
                else if(tag.equals("ol")) parent="ol";
                if(tag.equals("li")){
                    if(parent.equals("ul")){
                        if(first){
                            output.append("\n•");
                            first= false;
                        }else{
                            first = true;
                        }
                    }
                    else{
                        if(first){
                            output.append("\n"+index+". ");
                            first= false;
                            index++;
                        }else{
                            first = true;
                        }
                    }
                }
            }
        })));
        detailsTextView.setMovementMethod(LinkMovementMethod.getInstance()); // enables anchor tags
    }

    @Override
    public int getItemCount() {
        return 1;
    }
}