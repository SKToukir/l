package bdtube.vumobile.com.bdtube;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.longtailvideo.jwplayer.JWPlayerView;
import com.longtailvideo.jwplayer.configuration.PlayerConfig;
import com.longtailvideo.jwplayer.configuration.Skin;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bdtube.vumobile.com.bdtube.Adapter.RelatedItemsAdapter;
import bdtube.vumobile.com.bdtube.Api.Config;
import bdtube.vumobile.com.bdtube.App.DividerItemDecoration;
import bdtube.vumobile.com.bdtube.App.RecyclerTouchListener;
import bdtube.vumobile.com.bdtube.App.SubcriptionClass;
import bdtube.vumobile.com.bdtube.App.UserInfo;
import bdtube.vumobile.com.bdtube.Model.RelatedItemClass;
import bdtube.vumobile.com.bdtube.notification.PHPRequest;

public class VideoPreviewActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener, View.OnClickListener {

    public static boolean isNotific = false;

    public static String notification_vUrl = "";

    public String pushResponseUrl = "http://203.76.126.210/sticker_gcm_server_bdtube/push_response.php";

    PHPRequest phpRequest = new PHPRequest();
    private int c;
    private ImageView btnLike, btnFvrt;
    JWPlayerView playerView;
    String VideoURL = "http://wap.shabox.mobi/CMS/Content/Graphics/Video%20Clips/D800x600/Ami_Jare_by_DJ_Rahat_N_Reshmi.mp4";
    public static String ContentCategoryCode="",ContentCode="",ContentTitle="",ContentType="",physicalFileName="",artist="",ContentZedCode="",
            totalLike="",totalView="",imgUrl="",relatedContentUrl="",relatedCatCode="" , duration = "", info = "", genre = "", isHD = "";

    private GridLayoutManager mLayoutManager;

    private List<RelatedItemClass> relatedItemClassList = new ArrayList<>();
    private RelatedItemClass relatedItemClass;

    private RecyclerView recycler_view_related_items;
    private RecyclerView.Adapter adapter;

    private SwipeRefreshLayout swipeRefreshLayout;

    private TextView txtTotalLikes, txtTotalViews, txtDuration, txtInfo, txtGenre;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_preview);

        initUI();

        setUpVideoUrl();

        initRecycler();

        try{
            giveLike("wifi","View",ContentCode);
            //giveView("wifi","View",ContentCode);
        }catch (Exception e){
            e.printStackTrace();
        }

        swipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(true);
                parseRelatedItems(1);
            }
        });

        recycler_view_related_items.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {

                if (dy>0){
                    c = mLayoutManager.findLastVisibleItemPosition()+1;
                    if (c==relatedItemClassList.size() && !swipeRefreshLayout.isRefreshing()){
                        parseRelatedItems(c+1);
                    }
                }
            }
        });

        recycler_view_related_items.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), recycler_view_related_items, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                RelatedItemClass newVideoClass = relatedItemClassList.get(position);
                SubcriptionClass.ContentCategoryCode = newVideoClass.getContentCategoryCode();
                SubcriptionClass.ContentCode = newVideoClass.getContentCode();
                SubcriptionClass.ContentTitle = newVideoClass.getContentTitle();
                SubcriptionClass.ContentType = newVideoClass.getContentType();
                SubcriptionClass.physicalFileName = newVideoClass.getVideoURL();
                SubcriptionClass.artist = newVideoClass.getArtist();
                SubcriptionClass.ContentZedCode = newVideoClass.getContentZedCode();
                SubcriptionClass.totalLike = newVideoClass.getTotalLike();
                SubcriptionClass.totalView = newVideoClass.getTotalView();
                SubcriptionClass.imgUrl = newVideoClass.getImageUrl();
                SubcriptionClass.info = newVideoClass.getInfo();
                SubcriptionClass.genre = newVideoClass.getGenre();
                SubcriptionClass.duration = newVideoClass.getDuration();
                SubcriptionClass.isHD = newVideoClass.getIsHd();
                SubcriptionClass.relatedContentUrl = Config.URL_NEW_VIDEO;
                SubcriptionClass.relatedCatCode = relatedCatCode;
                new SubcriptionClass(VideoPreviewActivity.this).checkSubscription();
                finish();
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));


        Log.d("isNotific",String.valueOf(isNotific));

        if (isNotific){
            new SendLaunchPushResponse().execute();
        }


    }

    private void parseRelatedItems(int items) {
        Log.d("catcode",relatedCatCode);
        swipeRefreshLayout.setRefreshing(true);

        adapter.notifyDataSetChanged();

        JSONObject js = new JSONObject();
        try {
            js.put("CatCode", relatedCatCode);
            js.put("PageTotal",items);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        final String requestBody = js.toString();
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.POST, relatedContentUrl, requestBody, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                swipeRefreshLayout.setRefreshing(false);
                Log.d("JSON response", response.toString());

                try {
                    // Parsing json array response
                    // loop through each json object
                    Log.d("LOGGGGG","Parse Complete");


                    swipeRefreshLayout.setVisibility(View.VISIBLE);

                    for (int i = 0; i < response.length(); i++) {

                        JSONObject homeURL = (JSONObject) response
                                .get(i);
                        relatedItemClass = new RelatedItemClass();
                        String TimeStamp = homeURL.getString("TimeStamp");
                        String ContentCode = homeURL.getString("ContentCode");
                        String ContentCategoryCode = homeURL.getString("ContentCategoryCode");
                        String ContentTitle = homeURL.getString("ContentTitle");
                        String PreviewURL = homeURL.getString("BigPreview");
                        String VideoURL = homeURL.getString("PhysicalFileName");
                        String ContentType = homeURL.getString("ContentType");
                        String Value = homeURL.getString("Value");
                        String Artist = homeURL.getString("Artist");
                        String ContentZedCode = homeURL.getString("ContentZedCode");
                        String totalLike = homeURL.getString("totalLike");
                        String totalView = homeURL.getString("totalView");
                        String imgUrl = homeURL.getString("imageUrl");
                        String info = homeURL.getString(Config.INFO);
                        String duration = homeURL.getString(Config.DURATION);
                        String genre = homeURL.getString(Config.GENRE);
                        String isHD = homeURL.getString(Config.isHd);

                        relatedItemClass.setContentCategoryCode(ContentCategoryCode);
                        relatedItemClass.setContentCode(ContentCode);
                        relatedItemClass.setTimeStamp(TimeStamp);
                        relatedItemClass.setContentTitle(ContentTitle);
                        relatedItemClass.setPreviewURL(PreviewURL);
                        relatedItemClass.setVideoURL(VideoURL);
                        relatedItemClass.setContentType(ContentType);
                        relatedItemClass.setValue(Value);
                        relatedItemClass.setArtist(Artist);
                        relatedItemClass.setContentZedCode(ContentZedCode);
                        relatedItemClass.setTotalLike(totalLike);
                        relatedItemClass.setTotalView(totalView);
                        relatedItemClass.setImageUrl(imgUrl);
                        relatedItemClass.setInfo(info);
                        relatedItemClass.setGenre(genre);
                        relatedItemClass.setDuration(duration);
                        relatedItemClass.setIsHd(isHD);

                        // Log.d("JSON data", PreviewURL + "    ContentCategoryCode: " + PreviewURLserch);

                        relatedItemClassList.add(relatedItemClass);
                    }


                    adapter.notifyDataSetChanged();

                } catch (JSONException e) {
                    e.printStackTrace();

                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Response", "o"+error.getMessage());
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        //StringRequest request = new StringRequest()

        RequestQueue requestQueue = Volley.newRequestQueue(VideoPreviewActivity.this);
        request.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        //Adding request to the queue
        requestQueue.add(request);
    }

    private void initRecycler() {
        adapter = new RelatedItemsAdapter(VideoPreviewActivity.this,relatedItemClassList);
        recycler_view_related_items = (RecyclerView) findViewById(R.id.recycler_view_related_items);
        recycler_view_related_items.setAdapter(adapter);
        recycler_view_related_items.setNestedScrollingEnabled(false);
        mLayoutManager = new GridLayoutManager(getApplicationContext(),1);
        recycler_view_related_items.setLayoutManager(mLayoutManager);
        recycler_view_related_items.setItemAnimator(new DefaultItemAnimator());
        recycler_view_related_items.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
    }

    private void initUI() {

        btnFvrt = (ImageView) findViewById(R.id.btnFvrt);
        btnLike = (ImageView) findViewById(R.id.btnLike);
        txtTotalLikes = (TextView) findViewById(R.id.txtTotalLikes);
        txtTotalViews = (TextView) findViewById(R.id.txtTotalViews);
        txtDuration = (TextView) findViewById(R.id.txtDuration);
        txtInfo = (TextView) findViewById(R.id.txtInfo);
        txtGenre = (TextView) findViewById(R.id.txtGenre);

        btnFvrt.setOnClickListener(this);
        btnLike.setOnClickListener(this);
        txtTotalViews.setOnClickListener(this);

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout_cat_wise);
        swipeRefreshLayout.setOnRefreshListener(this);

        txtGenre.setText(genre);
        txtInfo.setText(info);
        txtDuration.setText(duration);
        //txtTotalViews.setText(totalView);
        txtTotalLikes.setText(totalLike);
    }

    private void setUpVideoUrl() {

        if (ContentType.equalsIgnoreCase("FV")) {
            String videoURL = "http://wap.shabox.mobi/CMS/Content/Graphics/FullVideo/D480x320/" + physicalFileName + ".mp4";
            String url = videoURL.replaceAll(" ","%20");
            playVideo(url);
        } else {
            String videoURL = "http://wap.shabox.mobi/CMS/Content/Graphics/Video Clips/D480x320/" + physicalFileName + ".mp4";
            String url = videoURL.replaceAll(" ","%20");
            playVideo(url);
        }



    }

    private void playVideo(String url) {



        Log.d("COntentImage",imgUrl);
       try{
           if (isHD.equals("1")){
               playerView = (JWPlayerView) findViewById(R.id.VideoView);
               PlayerConfig playerConfig = new PlayerConfig.Builder()
                       .file(url).controls(true).skin(Skin.SEVEN).logoFile("http://vumobile.biz/v/lk.png").logoPosition(PlayerConfig.LOGO_POSITION_BOTTOM_RIGHT).logoHide(true)
                       .timeSliderAbove(true).image(imgUrl)
                       .build();
               playerView = new JWPlayerView(VideoPreviewActivity.this, playerConfig);
               ViewGroup jwPlayerViewContainer = (ViewGroup) findViewById(R.id.containers);
               jwPlayerViewContainer.addView(playerView);
               playerView.setMute(false);
           }else {
               playerView = (JWPlayerView) findViewById(R.id.VideoView);
               PlayerConfig playerConfig = new PlayerConfig.Builder()
                       .file(url).controls(true).skin(Skin.SEVEN)
                       .timeSliderAbove(true).stretching("exactfit").image(imgUrl)
                       .build();
               playerView = new JWPlayerView(VideoPreviewActivity.this, playerConfig);
               ViewGroup jwPlayerViewContainer = (ViewGroup) findViewById(R.id.containers);
               jwPlayerViewContainer.addView(playerView);
               playerView.setMute(false);
           }
       }catch (Exception e){
           e.printStackTrace();
           Log.d("EXOERROR","EXOERROR");
       }

    }

    @Override
    public void onRefresh() {
        parseRelatedItems(1);
    }

    private void giveLike(String finalMobileNumber, final String like, String contentCode) {

        Log.d("kkkkkkkk",finalMobileNumber+" "+like+" "+contentCode);
        String url = "http://wap.shabox.mobi/bdnewapi/Data/LikeFav";
        JSONObject js = new JSONObject();
        try {
            js.put("MSISDN", finalMobileNumber);
            js.put("Type", like);
            js.put("ContentCode", contentCode);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Make request for JSONObject
        JsonObjectRequest jsonObjReq = new JsonObjectRequest(
                Request.Method.POST, url, js,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("response", response.toString());
                        try {
                            String result = response.getString("result");

                            if (result.equals("Success")){

                                try{
                                    if (like.equalsIgnoreCase("Like")){
                                        Log.d("Likeresponse", response.toString());
                                        int likes = Integer.parseInt(totalLike);
                                        likes = likes+1;
                                        String Likes = String.valueOf(likes);
                                        totalLike = String.valueOf(likes);
                                        Log.d("Likeresponse", String.valueOf(likes));
                                        txtTotalLikes.setText(Likes);
                                    }else if (like.equals("View")){
                                        int views = Integer.parseInt(totalView);
                                        views = views+1;
                                        String Views = String.valueOf(views);
                                        totalView = String.valueOf(views);
                                        txtTotalViews.setText(Views);
                                    }else {
                                        Log.d("SAVEFORFAV","SAVEFORFAV");
                                        Toast.makeText(getApplicationContext(),"Add as favourite!",Toast.LENGTH_LONG).show();
                                    }
                                }catch (NumberFormatException e){


                                }
                            }else {
                                Toast.makeText(getApplicationContext(),"Can not find your mobile number!",Toast.LENGTH_LONG).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d("response", "Error: " + error.getMessage());
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/json; charset=utf-8");
                return headers;
            }


        };

        // Adding request to request queue
        RequestQueue requestQueue = Volley.newRequestQueue(VideoPreviewActivity.this);
        jsonObjReq.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        //Adding request to the queue
        requestQueue.add(jsonObjReq);

    }

    @Override
    public void onClick(View v) {

        switch (v.getId()){

            case R.id.btnLike:
                giveLike(SplashActivity.MSISDN,"Like",ContentCode);
                break;
            case R.id.btnFvrt:
                giveLike(SplashActivity.MSISDN,"Fav",ContentCode);
                break;
        }
    }

    private class SendLaunchPushResponse extends AsyncTask<String, String, String> {


        UserInfo userinfo = new UserInfo();
        String HS_MANUFAC_ = userinfo.deviceMANUFACTURER(VideoPreviewActivity.this);
        String HS_MOD_ = userinfo.deviceModel(VideoPreviewActivity.this);
        String user_email = userinfo.userEmail(VideoPreviewActivity.this);

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();


            // detect MSISDN when notification launched
            // detect MSISDN when notification launched


        }

        @Override
        protected String doInBackground(String... args) {
            // TODO Auto-generated method stub

            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("email", user_email));
            params.add(new BasicNameValuePair("action", "launch"));
            params.add(new BasicNameValuePair("handset_name", HS_MANUFAC_));
            params.add(new BasicNameValuePair("handset_model", HS_MOD_));
            params.add(new BasicNameValuePair("msisdn", SplashActivity.MSISDN));

            // getting JSON Object
            // Note that create product url accepts POST method
            phpRequest.makeHttpRequest(pushResponseUrl, "POST", params);
            Log.d("Toukirul", pushResponseUrl + "params: " + params);
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        playerView.destroySurface();
        playerView.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        playerView.destroySurface();
        playerView.stop();
    }
}
