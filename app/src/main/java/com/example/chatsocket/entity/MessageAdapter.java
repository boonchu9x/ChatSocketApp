package com.example.chatsocket.entity;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.chatsocket.R;
import com.example.chatsocket.utils.ImageUtil;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;


public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<Message> mMessages;
    private Context context;

    private OnClickItem onClickMessage;

    public MessageAdapter(Context context, List<Message> messages, OnClickItem onClickMessage) {
        this.context = context;
        mMessages = messages;
        this.onClickMessage = onClickMessage;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case Message.TYPE_MESSAGE_ME:
                View itemMe = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_me, parent, false);
                return new ViewHoderMessage(itemMe);
            case Message.TYPE_MESSAGE_YOU:
                View itemYou = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_you, parent, false);
                return new ViewHoderMessage(itemYou);
            case Message.TYPE_ACTION:
                View itemAction = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_action, parent, false);
                return new ViewHolder(itemAction);
            default:
                View itemLog = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_log, parent, false);
                return new ViewHolder(itemLog);
        }

    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == Message.TYPE_MESSAGE_ME || getItemViewType(position) == Message.TYPE_MESSAGE_YOU) {
            ((ViewHoderMessage) holder).setTransaction(mMessages.get(position), getItemViewType(position - 1) == getItemViewType(position), getItemViewType(position));
        } else {
            Message message = mMessages.get(position);
            ((ViewHolder) holder).setMessage(message.getMessage());
            ((ViewHolder) holder).setUsername(message.getUsername());
        }

    }

    @Override
    public int getItemCount() {
        return mMessages.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mMessages.get(position).getType();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView mUsernameView;
        private TextView mMessageView;

        ViewHolder(View itemView) {
            super(itemView);

            mUsernameView = itemView.findViewById(R.id.username);
            mMessageView = itemView.findViewById(R.id.message);
        }

        void setUsername(String username) {
            if (null == mUsernameView) return;
            mUsernameView.setText(username);
        }

        void setMessage(String message) {
            if (null == mMessageView) return;
            mMessageView.setText(message);
        }
    }


    public class ViewHoderMessage extends RecyclerView.ViewHolder {

        @BindView(R.id.ln_message)
        LinearLayout lnMessage;
        @BindView(R.id.tvTime)
        TextView tvTime;
        @BindView(R.id.tvMsg)
        TextView tvMessage;
        @BindView(R.id.imgAvatar)
        CircleImageView imgAvatar;
        @BindView(R.id.card_image)
        CardView cardImage;
        @BindView(R.id.img_message)
        ImageView imgMessage;

        ViewHoderMessage(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
        }

        void setTransaction(Message message, boolean notshow, int type) {
            if (message != null) {
                cardImage.setVisibility(View.GONE);

                if (message.getByteImage() != null && message.getByteImage().length > 0) {
                    cardImage.setVisibility(View.VISIBLE);
                    imgMessage.setImageBitmap(ImageUtil.getBitmapFromByte(message.getByteImage()));
                }
                if (notshow) {
                    RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) lnMessage.getLayoutParams();
                    layoutParams.setMargins(layoutParams.getMarginStart(), context.getResources().getDimensionPixelSize(R.dimen.border_corner_normal), layoutParams.getMarginEnd(), 0);
                    lnMessage.setLayoutParams(layoutParams);
                    tvTime.setVisibility(View.GONE);
                    tvMessage.setBackground(type == 0 ? ContextCompat.getDrawable(context, R.drawable.border_item_message_me_continuity) : ContextCompat.getDrawable(context, R.drawable.border_item_message_you_continuity));
                    imgAvatar.setVisibility(View.INVISIBLE);
                }
                tvTime.setText(message.getUsername());
                if (TextUtils.isEmpty(message.getMessage())) tvMessage.setVisibility(View.GONE);
                tvMessage.setText(message.getMessage());

                tvMessage.setOnLongClickListener(view -> {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText("Copied", message.getMessage());
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show();
                    }
                    return false;
                });

                cardImage.setOnClickListener(view -> {
                    if (onClickMessage != null) {
                        onClickMessage.onClick();
                    }
                });

            }
        }

    }

    public interface OnClickItem {
        void onClick();
    }

}
