package demo.ethings.com.ethingsble.adapter;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import demo.ethings.com.ethingsble.R;
import demo.ethings.com.ethingsble.model.TagDevice;

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
        ImageView iv_state;

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
            holder.iv_state = (ImageView) vi.findViewById(R.id.iv_state);

            /************  Set holder with LayoutInflater ************/
            vi.setTag(holder);
        } else holder = (ViewHolder) vi.getTag();

        if (devices != null) {
            TagDevice tag = devices.get(position);
            holder.name.setText(tag.getName());
            holder.intensity.setText(tag.getRSSI() + "");
            holder.pin.setText(tag.getPin() + "%");
            if (tag.getState()) {
                holder.iv_state.setImageResource(R.drawable.state_on);
            } else {
                holder.iv_state.setImageResource(R.drawable.state_off);
            }
        }

        return vi;
    }
}
