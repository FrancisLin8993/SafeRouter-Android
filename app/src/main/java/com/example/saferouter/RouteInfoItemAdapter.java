package com.example.saferouter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.saferouter.model.RouteInfoItem;

import java.util.List;

public class RouteInfoItemAdapter extends RecyclerView.Adapter<RouteInfoItemAdapter.RouteInfoViewHolder> {

    private List<RouteInfoItem> routeInfoItemList;

    public RouteInfoItemAdapter(List<RouteInfoItem> routeInfoItemList) {
        this.routeInfoItemList = routeInfoItemList;
    }

    public class RouteInfoViewHolder extends RecyclerView.ViewHolder{
        public TextView safetyScore, routeNo, duration, distance;

        public RouteInfoViewHolder(@NonNull View itemView) {
            super(itemView);
            safetyScore = itemView.findViewById(R.id.safetyScore);
            routeNo = itemView.findViewById(R.id.routeNo);
            duration = itemView.findViewById(R.id.duration);
            distance = itemView.findViewById(R.id.distance);
        }
    }


    @NonNull
    @Override
    public RouteInfoViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.route_info_item, viewGroup, false);

        return new RouteInfoViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull RouteInfoViewHolder routeInfoViewHolder, int i) {
        RouteInfoItem routeInfoItem = routeInfoItemList.get(i);
        routeInfoViewHolder.safetyScore.setText(routeInfoItem.getRiskScore());
        routeInfoViewHolder.routeNo.setText(routeInfoItem.getRouteNo());
        routeInfoViewHolder.distance.setText(routeInfoItem.getDistance());
        routeInfoViewHolder.duration.setText(routeInfoItem.getDuration());
    }

    @Override
    public int getItemCount() {
        return routeInfoItemList.size();
    }
}
