package com.emanga.emanga.app.activities;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.emanga.emanga.app.R;
import com.emanga.emanga.app.adapters.ImagePagerAdapter;
import com.emanga.emanga.app.controllers.App;
import com.emanga.emanga.app.database.OrmliteFragmentActivity;
import com.emanga.emanga.app.models.Chapter;
import com.emanga.emanga.app.models.Manga;
import com.emanga.emanga.app.models.Page;
import com.emanga.emanga.app.requests.ChapterRequest;
import com.emanga.emanga.app.utils.CustomViewPager;
import com.emanga.emanga.app.utils.Internet;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.QueryBuilder;

import java.sql.SQLException;
import java.util.Date;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 */
public class ReaderActivity extends OrmliteFragmentActivity {

	public static final String TAG = "ReaderActivity";

	public static final String ACTION_OPEN_CHAPTER = "com.emanga.emanga.app.intent.action"
            + TAG + ".openChapter";
    public static final String ACTION_OPEN_CHAPTER_NUMBER = "com.emanga.emanga.app.intent.action"
            + TAG + ".openChapterNumber";
    public static final String ACTION_OPEN_MANGA = "com.emanga.emanga.app.intent.action"
			+ TAG + ".openManga";
	
	// Current manga and chapter
    private Page mark;
	private Chapter mChapter;
    private Manga mManga;
	public ImagePagerAdapter mAdapter;
	private CustomViewPager mPager;

    private ChapterRequest request;
    private Boolean asked;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();

        if(extras.get(ACTION_OPEN_CHAPTER) != null) {
            mChapter = (Chapter) extras.get(ACTION_OPEN_CHAPTER);
            mManga = mChapter.manga;

            try {
                // Query for the last page read
                QueryBuilder<Page, String> qBp = getHelper()
                        .getPageRunDao().queryBuilder();
                qBp.where().eq(Page.CHAPTER_COLUMN_NAME, mChapter._id);
                qBp.orderBy(Page.READ_COLUMN_NAME, false);
                qBp.limit(1L);
                mark = qBp.queryForFirst();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            if (mark != null) {
                Log.d(TAG, "Last page read: " + mark.toString());
            } else {
                Log.d(TAG, "There is not a last page read");
            }

            askChapter(mChapter.number);
        } else {

            mManga = (Manga) extras.get(ACTION_OPEN_MANGA);
            askChapter(extras.getInt(ACTION_OPEN_CHAPTER_NUMBER,1));
        }

        setContentView(R.layout.activity_reader);
        mPager = (CustomViewPager) findViewById(R.id.fullscreen_pager);

		mAdapter = new ImagePagerAdapter(getSupportFragmentManager());
		mPager.setAdapter(mAdapter);

		mPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            public void onPageSelected(int position) {
				// Save when a page is read
				Page page = mAdapter.pages.get(position);
                Log.d(TAG, "Saving " + page.number + " as mark");
                page.read = new Date();
                new SaveMark().execute(page);

                // When only rest 5 pages for the end, it loads the next chapter
                // Position begins from 0 whereas that size() does it from 1
                if((mAdapter.pages.size() - 1) - position < 5 && !asked ){
                    askChapter(mChapter.number + 1);
                }
			}

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
	}

	@Override
	public void onDestroy(){
        request.cancel();
        super.onDestroy();
	}

    private void askChapter(final int number){
        asked = true;
        final Activity activity = this;
        Log.d(TAG, "Request to url: " + Internet.HOST + "manga/" + mManga._id + "/chapter/" + number);
        request = new ChapterRequest(
                Request.Method.GET,
                Internet.HOST + "manga/" + mManga._id + "/chapter/" + number,
                new Response.Listener<Chapter>() {
                    @Override
                    public void onResponse(final Chapter chapter){
                        if(chapter._id != null){
                            Log.d(TAG, "Chapter recived:\n" + chapter.toString());
                            // Add new pages to adapter
                            mAdapter.pages.addAll(chapter.pages);
                            mChapter = chapter;
                            mAdapter.notifyDataSetChanged();

                            // Set as read the chapter
                            mChapter.read = new Date();
                            mChapter.manga = mManga;

                            // If is a resume reading
                            if(mark != null){
                                Log.d(TAG, "Set current page: " + (mark.number - 1) );
                                Log.d(TAG, "Page adapter has " + mAdapter.getCount() + " pages loaded");
                                mPager.setCurrentItem(mark.number - 1);
                                mark = null;
                            }

                            new AsyncTask<Void,Void,Void>(){
                                @Override
                                protected Void doInBackground(Void... voids) {
                                    Log.d(TAG, "Saving chapter as read");
                                    getHelper().getChapterRunDao().createOrUpdate(mChapter);
                                    return null;
                                }
                            }.execute();

                            asked = false;
                        } else {
                            Toast.makeText(activity, getResources().getString(R.string.chapter_not_found_error) + ": " + number, Toast.LENGTH_LONG).show();
                            Log.d(TAG, "The chapter: " + number + " doesn't exist");
                        }
                    }
                },
                null);

        request.setRetryPolicy(new DefaultRetryPolicy(
            2 * 60 * 1000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        );

        App.getInstance().addToRequestQueue(request, "Pages for chapter " + number);
    }

    class SaveMark extends AsyncTask<Page,Void,Void>{
        private RuntimeExceptionDao<Page,String> dao = getHelper().getPageRunDao();

        @Override
        protected Void doInBackground(Page... pages) {
            dao.createOrUpdate(pages[0]);
            return null;
        }
    }
}
