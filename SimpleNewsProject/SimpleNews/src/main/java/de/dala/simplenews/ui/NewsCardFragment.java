package de.dala.simplenews.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import androidrss.MediaEnclosure;
import androidrss.RSSConfig;
import androidrss.RSSFeed;
import androidrss.RSSItem;
import androidrss.RSSParser;
import de.dala.simplenews.R;
import de.dala.simplenews.database.DatabaseHandler;
import de.dala.simplenews.database.IDatabaseHandler;
import de.dala.simplenews.common.Category;
import de.dala.simplenews.common.Entry;
import de.dala.simplenews.common.Feed;
import de.dala.simplenews.common.NewsCard;
import de.dala.simplenews.network.NetworkCommunication;
import de.dala.simplenews.utilities.MyCardArrayAdapter;
import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.view.CardListView;

/**
 * Created by Daniel on 18.12.13.
 */
public class NewsCardFragment extends SherlockFragment {
    //TODO fragment performance
    private static final String ARG_CATEGORY = "category";
    public static final String IMAGE_JPEG = "image/jpeg";
    private CardListView mListView;
    private View undoBar;
    private MyCardArrayAdapter mCardArrayAdapter;
    private Category category;

    private List<Feed> feeds;
    private MainActivity activity;

    private IDatabaseHandler databaseHandler;
    private static long TIME_FOR_REFRESH = 1000 * 60  * 60; //one hour


    public static NewsCardFragment newInstance(Category category) {
        NewsCardFragment f = new NewsCardFragment();
        Bundle b = new Bundle();
        b.putSerializable(ARG_CATEGORY, category);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof MainActivity){
            this.activity = (MainActivity) activity;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        category = (Category) getArguments().getSerializable(ARG_CATEGORY);

        if (savedInstanceState != null){
            Object feedsObject = savedInstanceState.getSerializable("feeds");
            if (feedsObject != null){
                feeds = (ArrayList<Feed>) feedsObject;
            }
        }

        databaseHandler = DatabaseHandler.getInstance(getActivity());

        if (feeds == null){
            feeds = databaseHandler.getFeeds(category.getId(), null);
        }
        View rootView = inflater.inflate(R.layout.list_base_different_inner, container, false);
        mListView = (CardListView) rootView.findViewById(R.id.card_list_base);
        undoBar = rootView.findViewById(R.id.list_card_undobar);
        initCardsAdapter();

        loadEntries();
        return rootView;
    }

    private void initCardsAdapter() {
        // Provide a custom adapter.
        // It is important to set the viewTypeCount
        // You have to provide in your card the type value with {@link Card#setType(int)} method.
        mCardArrayAdapter = new MyCardArrayAdapter(activity,new ArrayList<Card>());
        //mCardArrayAdapter.setInnerViewTypeCount(3);
        mCardArrayAdapter.setEnableUndo(true, undoBar);

        if (mListView!=null){
            mListView.setAdapter(mCardArrayAdapter);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                refreshFeeds();
                return true;

            default:
                break;
        }

        return false;
    }

    @Override
     public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.news, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    private void refreshFeeds(){
        mCardArrayAdapter.clear();
        databaseHandler.removeEntries(category.getId(), null, null);
        for (Feed feed : feeds){
            updateFeed(feed);
        }
        category.setLastUpdateTime(new Date().getTime());
        databaseHandler.updateCategoryTime(category.getId(), category.getLastUpdateTime());
    }

    private void loadEntries() {
        if (category.getLastUpdateTime() < new Date().getTime() - TIME_FOR_REFRESH){
            refreshFeeds();
        }else{
                List<Entry> entries = databaseHandler.getEntries(category.getId(), null);
                for (Entry entry : entries){
                    addEntryToCardAndDatabase(-1, entry, false);
                }
            }
    }

    private void updateFeed(final Feed feed) {
        if (activity != null){
            activity.showLoadingNews();
        }

        NetworkCommunication.loadRSSFeed(feed.getUrl(),new Response.Listener<String>() {
            @Override
            public void onResponse(String feedStringResult) {
                RSSParser parser = new RSSParser(new RSSConfig());
                try {
                    RSSFeed rssFeed = parser.parse(new ByteArrayInputStream(feedStringResult.getBytes("UTF-8")));
                    String title = rssFeed.getTitle();
                    if (rssFeed.getItems() != null){
                        for (RSSItem item : rssFeed.getItems()){
                            addRSSItemToEntry(item, feed.getId(), title);
                        }
                    }
                }catch (UnsupportedEncodingException ex){}
                finally {
                    activity.cancelLoadingNews();
                }
            }
        }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    activity.cancelLoadingNews();
                }
            });
    }


    private void addRSSItemToEntry(RSSItem item, long feedId, String source) {
        MediaEnclosure enclose = item.getEnclosure();
        String mediaUri = null;
        if (enclose != null){
            if (enclose.getMimeType().equals(IMAGE_JPEG)){
                 mediaUri = enclose.getUrl().toString();
            }
        }
        String url = item.getLink().toString();
        Date pubDate = item.getPubDate();
        Long time = null;
        if (pubDate != null){
            time = pubDate.getTime();
        }
        String desc = item.getDescription();
        if (desc != null) {
            desc = desc.replaceAll("\\<.*?>","").replaceAll("()", "");
        }

        Entry entry = new Entry(-1, feedId, category.getId(), item.getTitle(), desc, time, source, url, mediaUri);
        addEntryToCardAndDatabase(feedId, entry, true);
    }

    private void addEntryToCardAndDatabase(long feedId, final Entry entry, boolean toDatabase){
        final Card newsCard = new NewsCard(activity, entry, category);
        newsCard.setId(entry.getTitle());
        mCardArrayAdapter.add(newsCard);

        if (toDatabase){
            databaseHandler.addEntry(category.getId(), feedId, entry);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("feeds", (ArrayList<Feed>) feeds);
    }

}