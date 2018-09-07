package vn.com.phamvantruongit.apppaypal;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;


import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalItem;
import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalPaymentDetails;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;
import com.paypal.android.sdk.payments.PaymentConfirmation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements ProductListAdapter.ProductListAdapterListener {
    
    
    private static final String TAG=MainActivity.class.getName();
    private ListView listView;
    private Button btnCheckOut;
    private List<Product> productList;
    private List<PayPalItem> productsInCart =new ArrayList<>();
    private ProductListAdapter adapter;
    private ProgressDialog dialog;
    private static final  int REQUEST_CODE_PAYMENT=1;
    private static PayPalConfiguration payPalConfiguration=new PayPalConfiguration()
            .environment(Config.PAYPAL_ENVIRONMENT).clientId(Config.PAYPAL_CLIENT_ID);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    private void init() {
        listView=findViewById(R.id.list);
        btnCheckOut=findViewById(R.id.btnCheckout);
        productList=new ArrayList<>();
        adapter=new ProductListAdapter(this,productList,this);
        listView.setAdapter(adapter);

        dialog=new ProgressDialog(this);
        dialog.setCancelable(false);

        Intent intent = new Intent(this, PayPalService.class);
        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, payPalConfiguration);
        startService(intent);

        btnCheckOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(productList.size()>0){
                    launchPayPalPayment();
                }else {
                    Toast.makeText(getApplicationContext(), "Cart is empty! Please add few products to cart.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        fetchProducts();

    }

    private void fetchProducts() {
        dialog.setMessage("Fetching products...");
        showDialog();
        JsonObjectRequest jsonObjectRequest=new JsonObjectRequest(Request.Method.GET, Config.URL_PRODUCTS, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                Log.d(TAG,response.toString());
                try {
                    JSONArray products=response.getJSONArray("products");
                    for(int i=0;i<products.length();i++){
                        JSONObject product= (JSONObject) products.get(i);
                        String id=product.getString("id");
                        String name=product.getString("name");
                        String description=product.getString("description");
                        String image=product.getString("image");
                        BigDecimal price=new BigDecimal(product.getString("price"));
                        String sku=product.getString("sku");
                        Product p=new Product(id,name,description,image,price,sku);
                        productList.add(p);
                        adapter.notifyDataSetChanged();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Error:" +e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                hideDialog();

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(TAG,"Error:" +error.getMessage());
                Toast.makeText(MainActivity.this, "Error:" +error.getMessage(), Toast.LENGTH_SHORT).show();
                hideDialog();
            }
        });
        AppController.getInstance().addToRequestQueue(jsonObjectRequest);

    }

    private void hideDialog() {
        dialog.dismiss();
    }

    private void showDialog() {
        dialog.show();
    }

    private void verifyPaymentOnServer(final String paymentID, final String payment_client){
      dialog.setMessage("Verifying payment...");
      showDialog();
        StringRequest verifyReq=new StringRequest(Request.Method.POST, Config.URL_VERIFY_PAYMENT, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
               Log.d(TAG,"verify payment:" +response.toString());
                try {
                    JSONObject res=new JSONObject(response);
                    boolean error=res.getBoolean("error");
                    String message=res.getString("message");
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                    if(!error){
                        productsInCart.clear();
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                hideDialog();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG,"verify error" +error.getMessage()) ;
                Toast.makeText(MainActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                hideDialog();
            }
        }){
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String,String> params=new HashMap<>();
                params.put("paymentId",paymentID);
                params.put("paymentClientJson", payment_client);
                return params;
            }
        };
        AppController.getInstance().addToRequestQueue(verifyReq);
    }
    private void launchPayPalPayment() {
        PayPalPayment thingsToBuy=prepareFinalCart();
        Intent intent = new Intent(MainActivity.this, PaymentActivity.class);

        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, payPalConfiguration);

        intent.putExtra(PaymentActivity.EXTRA_PAYMENT, thingsToBuy);

        startActivityForResult(intent, REQUEST_CODE_PAYMENT);
    }

    private PayPalPayment prepareFinalCart() {
        PayPalItem[] items = new PayPalItem[productsInCart.size()];
        items = productsInCart.toArray(items);

        // Total amount
        BigDecimal subtotal = PayPalItem.getItemTotal(items);

        // If you have shipping cost, add it here
        BigDecimal shipping = new BigDecimal("0.0");

        // If you have tax, add it here
        BigDecimal tax = new BigDecimal("0.0");

        PayPalPaymentDetails paymentDetails = new PayPalPaymentDetails(
                shipping, subtotal, tax);

        BigDecimal amount = subtotal.add(shipping).add(tax);

        PayPalPayment payment = new PayPalPayment(
                amount,
                Config.DEFAULT_CURRENCY,
                "Description about transaction. This will be displayed to the user.",
                Config.PAYMENT_INTENT);

        payment.items(items).paymentDetails(paymentDetails);

        // Custom field like invoice_number etc.,
        payment.custom("This is text that will be associated with the payment that the app can use.");

        return payment;
    }

    @Override
    public void onAddToCartPressed(Product product) {
      PayPalItem item=new PayPalItem(product.getName(),1,product.getPrice(),Config.DEFAULT_CURRENCY,product.getSku());
      productsInCart.add(item);
        Toast.makeText(this, item.getName() + " added to cart!", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE_PAYMENT) {
            if (resultCode == Activity.RESULT_OK) {
                PaymentConfirmation confirm = data
                        .getParcelableExtra(PaymentActivity.EXTRA_RESULT_CONFIRMATION);
                if (confirm != null) {
                    try {
                        Log.e(TAG, confirm.toJSONObject().toString(4));
                        Log.e(TAG, confirm.getPayment().toJSONObject()
                                .toString(4));

                        String paymentId = confirm.toJSONObject()
                                .getJSONObject("response").getString("id");

                        String payment_client = confirm.getPayment()
                                .toJSONObject().toString();

                        Log.e(TAG, "paymentId: " + paymentId
                                + ", payment_json: " + payment_client);

                        // Now verify the payment on the server side
                        verifyPaymentOnServer(paymentId, payment_client);

                    } catch (JSONException e) {
                        Log.e(TAG, "an extremely unlikely failure occurred: ",
                                e);
                    }
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Log.e(TAG, "The user canceled.");
            } else if (resultCode == PaymentActivity.RESULT_EXTRAS_INVALID) {
                Log.e(TAG,
                        "An invalid Payment or PayPalConfiguration was submitted.");
            }
        }
    }
}
