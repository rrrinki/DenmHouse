package com.denimhouse.Fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.denimhouse.Activity.AsynResult;
import com.denimhouse.Activity.MainDashboardActivity;
import com.denimhouse.Adapters.TodayDealAdapter;
import com.denimhouse.DataBaseHelpers.DatabaseHelper;
import com.denimhouse.Models.AllProductsModel;
import com.denimhouse.Activity.ViewDetail;
import com.denimhouse.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vishal mahajan on 8/12/2017.
 */
public class TodayDealsFragment extends Fragment {

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private Handler mHandler;
    public int counter = 0;
    private RecyclerView recyclerView;
    private TodayDealAdapter madapter;
    private ArrayList<AllProductsModel> todayDealList;
    int position;
    TextView count;
    AllProductsModel todayDealModel;
    Context context;
    ProgressBar progressBar;
    AsynResult<AllProductsModel> asynResultListener;
    AsynResult<AllProductsModel> asynResultListenerMinus;
    List<AllProductsModel> todayDealModelList = new ArrayList<>();
    DatabaseHelper databaseHelper;
    public String parent_id = "2";
    String size;


    public TodayDealsFragment(Context context, AsynResult<AllProductsModel> asynResultListener) {
        // Required empty public constructor
        this.context = context;
        this.asynResultListener = asynResultListener;
       // this.asynResultDetailView = asynResultDetailView;
        this.asynResultListenerMinus = asynResultListenerMinus;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tab_fragment_2, container, false);
        recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        count = (TextView) view.findViewById(R.id.textCounter);
        progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        todayDealList = new ArrayList<>();
        databaseHelper = new DatabaseHelper(context);
        //  madapter = new TodayDealAdapter(getContext(), todayDealList, asynResultListener);

        setAdapter();
        progressBar.setVisibility(View.VISIBLE);
        todayDealProducts();
        mHandler = new Handler();
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setColorSchemeResources(
                R.color.refresh_progress_1,
                R.color.refresh_progress_1,
                R.color.refresh_progress_1);
        //mSwipeRefreshLayout.setColorSchemeColors(Color.BLUE, Color.BLUE, Color.BLUE, Color.BLUE);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mSwipeRefreshLayout.setRefreshing(true);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (todayDealList.size() > 0) {
                            todayDealList.clear();
                            todayDealProducts();
                            madapter.notifyDataSetChanged();
                        }

                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                }, 3000);

            }
        });
        return view;
    }


    public void setAdapter() {
        madapter = new TodayDealAdapter(getContext(), todayDealList, asynResultListener, asynResultListenerModel, asynResultListenerMinus, asynResultDetailView);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(madapter);
    }

    AsynResult<AllProductsModel> asynResultListenerModel = new AsynResult<AllProductsModel>() {
        @Override
        public void success(AllProductsModel todayDealModel) {
            todayDealModel.setParent_id("2");
            todayDealModelList.add(todayDealModel);
            Boolean isItemExist = false;
            List<AllProductsModel> model = new ArrayList<>();
            model = databaseHelper.getData();
            for (AllProductsModel allProductsModel : model) {
                if (todayDealModel.getParent_id().equals(allProductsModel.getParent_id()) && todayDealModel.getId().equals(allProductsModel.getId())) {
                    size = todayDealModel.getSize();
                    int old_value = todayDealModel.getItemCount();
                    int new_value = allProductsModel.getItemCount();
                    int total = old_value + new_value;
                    Double old_value_subtotal = todayDealModel.getSubTotal();
                    Double new_value_subtotal = allProductsModel.getSubTotal();
                    Double totalSubTotal = old_value_subtotal + new_value_subtotal;
                    todayDealModel.setItemCount(total);
                    todayDealModel.setSubTotal(totalSubTotal);
                    isItemExist = true;
                    break;
                } else {
                    isItemExist = false;
                }
            }
            if (isItemExist) {

                databaseHelper.updateRecord(todayDealModel);
            } else {
                databaseHelper.insert(todayDealModel, parent_id);
            }

            ((MainDashboardActivity) getActivity()).dispatchInformations2(todayDealModelList);
        }

        @Override
        public void failure(String error) {

        }

        @Override
        public void passItemValue(int value) {

        }
    };


    AsynResult<AllProductsModel> asynResultDetailView = new AsynResult<AllProductsModel>() {
        @Override
        public void success(AllProductsModel todayDealModel) {
            todayDealModel.setSubTotal(Double.valueOf(todayDealModel.getProductPrice()));
            Intent intent=new Intent(context.getApplicationContext(), ViewDetail.class);
            intent.putExtra("AllProductsModel",todayDealModel);
            intent.putExtra("Parent_id", "2");
            startActivity(intent);
        }

        @Override
        public void failure(String error) {

        }

        @Override
        public void passItemValue(int value) {

        }
    };

    public void todayDealProducts() {
        RequestQueue queue = Volley.newRequestQueue(getActivity().getApplicationContext());
        String URL = "http://vertosys.com/denimhouse/webservices/categorywiseproduct.php" + "?category=" + "today%20deals";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, URL, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                System.out.println(response);
                Log.e("Response", response.toString());
                progressBar.setVisibility(View.GONE);
                try {
                    response.getString("message");
                    Log.e("Data", response.getString("message"));
                    JSONArray jsonObj = new JSONArray(response.getString("data"));
                    for (int i = 0; i < jsonObj.length(); i++) {
                        JSONObject jsonObject1 = jsonObj.getJSONObject(i);
                        String id = jsonObject1.getString("id");
                        String productName = jsonObject1.getString("prod_name");
                        String categoryId = jsonObject1.getString("category_id");
                        String productPrice = jsonObject1.getString("prod_price");
                        String discountType = jsonObject1.getString("discount_type");
                        String discountValue = jsonObject1.getString("discount_value");
                        String discountPrice = jsonObject1.getString("discount_price");
                        String productDescription = jsonObject1.getString("prod_desc");
                        String productImage = jsonObject1.getString("prod_image");
                        String status = jsonObject1.getString("status");
                        String categoryName = jsonObject1.getString("category_name");

                        todayDealModel = new AllProductsModel(id, productName, productDescription, productPrice, productImage, 0, 0,null);
                        todayDealList.add(todayDealModel);

                    }
                    madapter.notifyDataSetChanged();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                });
        queue.add(jsonObjectRequest);
    }

}
