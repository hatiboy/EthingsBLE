package demo.ethings.com.ethingsble.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;

import adapter.TagDeviceAdapter;
import demo.ethings.com.ethingsble.R;
import model.TagDevice;
import ui.DeviceScanActivity;

public class ListTagActivity extends Activity {

    private ListView listTag;
    private Button addnewtag;
    ArrayList<TagDevice> devices;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
        setContentView(R.layout.activity_list_tag);

        listTag = (ListView)findViewById(R.id.list_tag);
        addnewtag = (Button)findViewById(R.id.btn_add_new_tag);

        devices = new ArrayList<TagDevice>();
        devices.add(new TagDevice("1", "Bag", 4 , 100));
        devices.add(new TagDevice("2", "Keys" , 5 , 90));
        devices.add(new TagDevice("3", "Iphone", 5 , 100));

        TagDeviceAdapter adapter = new TagDeviceAdapter(this, R.layout.item_tag_device, devices);
        listTag.setAdapter(adapter);

        addnewtag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(ListTagActivity.this, DeviceScanActivity.class);
                startActivity(i);
            }
        });
    }
}
