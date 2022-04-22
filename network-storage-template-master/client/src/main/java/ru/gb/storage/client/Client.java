package ru.gb.storage.client;


import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.ReferenceCountUtil;
import ru.gb.storage.common.handler.JsonDecoder;
import ru.gb.storage.common.handler.JsonEncoder;
import ru.gb.storage.common.message.*;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) {
        new Client().start();
    }

    public void start() {
        final NioEventLoopGroup group = new NioEventLoopGroup(1);
        try {
            Bootstrap bootstrap = new Bootstrap()
                    .group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(
                                    new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 3, 0, 3),
                                    new LengthFieldPrepender(3),
                                    new JsonDecoder(),
                                    new JsonEncoder(),
                                    new SimpleChannelInboundHandler<Message>() {
                                        @Override
                                        public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                            final FileRequestMessage frm = new FileRequestMessage();
                                            frm.setPath("C:/Users/Fokusmod/Desktop/Screenshots.zip");
                                            ctx.writeAndFlush(frm);
                                        }

                                        @Override
                                        protected void channelRead0(ChannelHandlerContext ctx, Message msg) {
                                            if (msg instanceof FileContentMessage) {
                                                System.out.println("FileContentMessage");
                                                FileContentMessage fcm =  (FileContentMessage) msg;
                                                try (RandomAccessFile randomAccessFile = new RandomAccessFile("C:/Users/Fokusmod/Desktop/TextScreenshots.zip", "rw")) {
                                                    randomAccessFile.seek(fcm.getStartPosition());
                                                    randomAccessFile.write(fcm.getContent());
                                                    if (fcm.isLastPosition()) {
                                                        ctx.close();
                                                    }
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }

                                            }
                                        }
                                    }
                            );
                        }
                    });

            System.out.println("Client started");
            Channel channel = bootstrap.connect("localhost", 9000).sync().channel();
            channel.closeFuture().sync();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
    }

}
