package com.example.GamaPOS_V2S;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ButtonViewHolder> {
    private Context context;
    private List<Object> items;
    private OnItemClickListener onItemClickListener;
    private int selectedPosition = RecyclerView.NO_POSITION;
    private boolean isProduct;  // 用來區分商品按鈕

    public interface OnItemClickListener {
        void onItemClick(Object item);
    }

    public CategoryAdapter(Context context, List<Object> items, OnItemClickListener onItemClickListener, boolean isProduct) {
        this.context = context;
        this.items = items;
        this.onItemClickListener = onItemClickListener;
        this.isProduct = isProduct;
    }

    @NonNull
    @Override
    public ButtonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Button button = new Button(parent.getContext());
        RecyclerView.LayoutParams params;

        if (isProduct) {
            // 商品按鈕的尺寸設置
            params = new RecyclerView.LayoutParams(337, 180);
            button.setBackgroundResource(R.drawable.button_background_product); // 使用商品背景
        } else {
            // 主類和子類按鈕的尺寸設置
            params = new RecyclerView.LayoutParams(176, 156);
            button.setBackgroundResource(R.drawable.category_button_background);
        }

        params.bottomMargin = 16; // 設置向下的外距為16px
        button.setLayoutParams(params);
        button.setTextColor(Color.BLACK);
        button.setTextSize(TypedValue.COMPLEX_UNIT_PX, 32);  // 設置字體大小為32px
        return new ButtonViewHolder(button);
    }

    @Override
    public void onBindViewHolder(@NonNull ButtonViewHolder holder, int position) {
        Object item = items.get(position);

        if (item instanceof Category) {
            Category category = (Category) item;
            holder.button.setText(category.getName());
        } else if (item instanceof SubCategory) {
            holder.button.setText(((SubCategory) item).getName());
        } else if (item instanceof Product) {
            Product product = (Product) item;
            holder.button.setText(product.getProductName() + "\n" + product.getPrice() + "元");
            // 設置點擊事件來顯示產品對話框
            holder.button.setOnClickListener(v -> {
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClick(product);
                }
            });
        }

        if (!isProduct) {
            holder.button.setSelected(position == selectedPosition);
        }

        holder.button.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(item);
            }

            if (!isProduct) {
                // 更新選擇狀態
                notifyItemChanged(selectedPosition);
                selectedPosition = holder.getAdapterPosition();
                notifyItemChanged(selectedPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ButtonViewHolder extends RecyclerView.ViewHolder {
        Button button;

        ButtonViewHolder(@NonNull Button itemView) {
            super(itemView);
            button = itemView;
        }
    }
}

