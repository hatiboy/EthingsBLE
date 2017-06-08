package adapter;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import demo.ethings.com.ethingsble.R;
import model.TagDevice;

/**
 * Created by dangv on 26/05/2017.
 */

public class TagDeviceAdapter extends ArrayAdapter<TagDevice> {
    private Activity context;
    private ArrayList<TagDevice> devices;
    private static LayoutInflater inflater = null;

    public TagDeviceAdapter(@NonNull Activity context, @LayoutRes int resource, @NonNull ArrayList<TagDevice> devices) {
        super(context, resource, devices);
        this.context = context;
        this.devices = devices;
        inflater = (LayoutInflater) context.
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public static class ViewHolder {

        public TextView name;
        public TextView intensity;
        public TextView pin;

    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View vi = convertView;
        ViewHolder holder;

        if (vi == null) {

            /****** Inflate tabitem.xml file for each row ( Defined below ) *******/
            vi = inflater.inflate(R.layout.item_tag_device, null);
            /****** View Holder Object to contain tabitem.xml file elements ******/
            holder = new ViewHolder();
            holder.name = (TextView) vi.findViewById(R.id.tv_name_tag);
            holder.intensity = (TextView) vi.findViewById(R.id.tv_intensity);
            holder.pin = (TextView) vi.findViewById(R.id.tv_pin);

            /************  Set holder with LayoutInflater ************/
            vi.setTag(holder);
        } else holder = (ViewHolder) vi.getTag();

        if (devices != null) {
            TagDevice tag = devices.get(position);
            holder.name.setText(tag.getName());
            holder.intensity.setText(tag.getIntensity() + " pin");
            holder.pin.setText(tag.getPin() + "%");
            vi.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(context.getApplicationContext(), "Coming soon", Toast.LENGTH_SHORT).show();
                }
            });
        }

        return vi;
    }
}
