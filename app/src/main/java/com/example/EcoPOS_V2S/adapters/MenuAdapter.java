package com.example.EcoPOS_V2S.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.function.Consumer;

import com.example.EcoPOS_V2S.R;
import com.example.EcoPOS_V2S.activities.MainActivity;

public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.ButtonViewHolder> {
    private List<String> categories;
    private Context context;
    private OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClicked(String category);
    }

    public MenuAdapter(List<String> categories, Context context, OnCategoryClickListener listener) {
        this.categories = categories;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ButtonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Button button = new Button(parent.getContext());
        button.setLayoutParams(new RecyclerView.LayoutParams(337, 180)); // 按钮的尺寸
        return new ButtonViewHolder(button);
    }

    @Override
    public void onBindViewHolder(@NonNull ButtonViewHolder holder, int position) {
        String category = categories.get(position);
        holder.button.setText(category);
        holder.button.setOnClickListener(v -> listener.onCategoryClicked(category)); // 使用监听器
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class ButtonViewHolder extends RecyclerView.ViewHolder {
        Button button;

        ButtonViewHolder(@NonNull Button button) {
            super(button);
            this.button = button;
        }
    }


    private void showAlertDialog(String buttonName) {
        // 加载自定义布局
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_custom, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(dialogView);

        TextView tvButtonName = dialogView.findViewById(R.id.tvButtonName);
        EditText editTextQuantity = dialogView.findViewById(R.id.editTextQuantity);
        Button btnDecrease = dialogView.findViewById(R.id.btnDecrease);
        Button btnIncrease = dialogView.findViewById(R.id.btnIncrease);
        Button btnAddToCart = dialogView.findViewById(R.id.btnAddToCart);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        tvButtonName.setText(buttonName);  // 设置显示按钮名称

        btnDecrease.setOnClickListener(v -> {
            int quantity = Integer.parseInt(editTextQuantity.getText().toString());
            if (quantity > 1) {
                quantity--;
                editTextQuantity.setText(String.valueOf(quantity));
            }
        });

        btnIncrease.setOnClickListener(v -> {
            int quantity = Integer.parseInt(editTextQuantity.getText().toString());
            quantity++;
            editTextQuantity.setText(String.valueOf(quantity));
        });

        // 创建对话框实例
        AlertDialog dialog = builder.create();

        btnAddToCart.setOnClickListener(v -> {
            // 假设每次加入购物车增加1份
            ((com.example.EcoPOS_V2S.activities.MainActivity) context).updateOrderSummary(1);
            dialog.dismiss();
        });


        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

}
