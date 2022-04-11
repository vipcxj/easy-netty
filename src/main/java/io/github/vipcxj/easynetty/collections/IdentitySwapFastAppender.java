package io.github.vipcxj.easynetty.collections;

import io.github.vipcxj.easynetty.java8.collections.Java8IdentityFastAppender;
import io.github.vipcxj.easynetty.java9.collections.Java9IdentityFastAppender;
import io.github.vipcxj.easynetty.utils.Constants;

public class IdentitySwapFastAppender<T extends FastAppender.Node<T, T>> extends BaseSwapFastAppender<T, T> {

    public IdentitySwapFastAppender() {
        super(Constants.JAVA9 ? Java9IdentityFastAppender::new : Java8IdentityFastAppender::new);
    }
}
