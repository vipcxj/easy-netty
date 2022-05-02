package io.github.vipcxj.easynetty.buffer;

import io.github.vipcxj.easynetty.collections.UnsafeLinkedList;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;

public class StreamBuffer {
    private final UnsafeLinkedList<ByteBuf> buffers;
    private int readerIndex;
    private int writerIndex;
    private int markIndex;
    private UnsafeLinkedList.Node<ByteBuf> currentNode;
    private int currentOffset;

    public StreamBuffer() {
        this.buffers = new UnsafeLinkedList<>();
        this.readerIndex = 0;
        this.writerIndex = 0;
        this.markIndex = -1;
        this.currentNode = null;
        this.currentOffset = -1;
    }

    public void consumeBuffer(ByteBuf buf) {
        if (buf.isReadable()) {
            ByteBuf slicedBuf = buf.slice();
            if (currentNode == null) {
                buffers.addLast(slicedBuf);
                currentNode = buffers.getTail();
                currentOffset = currentNode.getData().readerIndex();
            } else {
                buffers.addLast(slicedBuf);
            }
            writerIndex += buf.writerIndex();
            buf.readerIndex(buf.writerIndex());
        }
    }

    public int readerIndex() {
        return readerIndex;
    }

    public void readerIndex(int readerIndex) {
        if (readerIndex < 0 || readerIndex > writerIndex) {
            throw new IndexOutOfBoundsException();
        }
        int offset = readerIndex - this.readerIndex;
        if (offset == 0) {
            return;
        }
        int currentReadable = moveToValid();
        if ((offset < 0 && currentOffset + offset >= 0) || (offset > 0 && offset <= currentReadable)) {
            this.currentOffset += offset;
        } else {
            UnsafeLinkedList.Node<ByteBuf> node = offset > 0 ? currentNode.getNext() : buffers.getHead();
            offset = offset > 0 ? this.readerIndex + currentReadable : 0;
            while (node != null) {
                ByteBuf current = node.getData();
                if (readerIndex < offset + current.writerIndex()) {
                    this.currentNode = node;
                    this.currentOffset = readerIndex - offset;
                    break;
                }
                offset += current.writerIndex();
                node = node.getNext();
            }
            if (node == null) {
                assert !buffers.isEmpty();
                this.currentNode = null;
                this.currentOffset = 0;
            }
        }
        this.readerIndex = readerIndex;
    }

    public int readableBytes() {
        return writerIndex - readerIndex;
    }

    public boolean isReadable(int length) {
        return readableBytes() >= length;
    }

    public boolean isReadable() {
        return isReadable(1);
    }

    private ByteBuf getCurrent() {
        return currentNode != null ? currentNode.getData() : null;
    }

    private ByteBuf safeGetCurrent() {
        ByteBuf current = getCurrent();
        assert current != null;
        return current;
    }

    private ByteBuf next() {
        currentNode = currentNode.getNext();
        ByteBuf current = getCurrent();
        if (current != null) {
            currentOffset = current.readerIndex();
        } else {
            currentOffset = buffers.isEmpty() ? -1 : 0;
        }
        return current;
    }

    private int currentReadableBytes() {
        return currentNode != null ? (currentNode.getData().writerIndex() - this.currentOffset) : 0;
    }

    private int moveToValid() {
        return moveToValid(currentReadableBytes());
    }

    private int moveToValid(int currentReadable) {
        if (currentNode == null) {
            return 0;
        }
        if (currentReadable == 0) {
            ByteBuf current = next();
            // at least 1
            return current != null ? current.writerIndex() - currentOffset : 0;
        } else {
            return currentReadable;
        }
    }

    public void mark() {
        markIndex = readerIndex;
    }

    public void cleanMark() {
        markIndex = -1;
    }

    public void resetMark() {
        if (markIndex == -1) {
            throw new IllegalStateException("The mark index is not set yet.");
        }
        UnsafeLinkedList.Node<ByteBuf> node = buffers.getHead();
        int offset = 0;
        while (node != null) {
            ByteBuf current = node.getData();
            if (markIndex < offset + current.writerIndex()) {
                readerIndex = markIndex;
                currentNode = node;
                currentOffset = markIndex - offset;
                return;
            }
            offset += current.writerIndex();
            node = node.getNext();
        }
        throw new IllegalStateException("This is impossible.");
    }

    public void freeSomeBytes() {
        int target = readerIndex;
        if (markIndex != -1 && markIndex < readerIndex) {
            target = markIndex;
        }
        moveToValid();
        if (currentNode == null) {
            release();
            return;
        }
        UnsafeLinkedList.Node<ByteBuf> node = buffers.getHead();
        int offset = 0;
        while (node != null) {
            ByteBuf current = node.getData();
            if (target < offset + current.writerIndex()) {
                break;
            }
            buffers.removeFirst();
            offset += current.writerIndex();
            current.release();
            node = node.getNext();
        }
        readerIndex -= offset;
        writerIndex -= offset;
        if (markIndex != -1) {
            markIndex -= offset;
        }
    }

    private byte getByte(boolean read) {
        if (isReadable(1)) {
            UnsafeLinkedList.Node<ByteBuf> currentNode = this.currentNode;
            int currentOffset = this.currentOffset;
            moveToValid();
            byte res = safeGetCurrent().getByte(this.currentOffset);
            if (read) {
                this.currentOffset++;
                readerIndex += 1;
            } else {
                this.currentNode = currentNode;
                this.currentOffset = currentOffset;
            }
            return res;
        }
        throw new IndexOutOfBoundsException();
    }

    public byte getByte() {
        return getByte(false);
    }

    public byte readByte() {
        return getByte(true);
    }

    private short getShort(boolean read) {
        if (isReadable(2)) {
            short res;
            UnsafeLinkedList.Node<ByteBuf> currentNode = this.currentNode;
            int currentOffset = this.currentOffset;
            int currentReadable = moveToValid();
            ByteBuf current = safeGetCurrent();
            if (currentReadable >= 2) {
                res = current.getShort(this.currentOffset);
                this.currentOffset += 2;
            } else {
                res = (short) (current.getByte(this.currentOffset) << 8);
                current = next();
                this.currentOffset = current.readerIndex();
                res |= current.getByte(this.currentOffset) & 0xff;
                this.currentOffset++;
            }
            if (read) {
                readerIndex += 2;
            } else {
                this.currentNode = currentNode;
                this.currentOffset = currentOffset;
            }
            return res;
        }
        throw new IndexOutOfBoundsException();
    }

    public short getShort() {
        return getShort(false);
    }

    public short readShort() {
        return getShort(true);
    }

    private int getInt(boolean read) {
        if (isReadable(4)) {
            int res;
            UnsafeLinkedList.Node<ByteBuf> currentNode = this.currentNode;
            int currentOffset = this.currentOffset;
            int currentReadable = moveToValid();
            if (currentReadable >= 4) {
                res = safeGetCurrent().getInt(this.currentOffset);
                this.currentOffset += 4;
            } else {
                res = 0;
                int toRead = currentReadable;
                int hasRead = 0;
                while (toRead > 0) {
                    for (int i = 1; i <= toRead; ++i) {
                        res |= safeGetCurrent().getByte(this.currentOffset) << ((4 - hasRead - i) * 8);
                        ++this.currentOffset;
                    }
                    currentReadable -= toRead;
                    int remaining = 4 - hasRead - toRead;
                    hasRead += toRead;
                    currentReadable = moveToValid(currentReadable);
                    toRead = Math.min(currentReadable, remaining);
                }
            }
            if (read) {
                readerIndex += 4;
            } else {
                this.currentNode = currentNode;
                this.currentOffset = currentOffset;
            }
            return res;
        }
        throw new IndexOutOfBoundsException();
    }

    public int getInt() {
        return getInt(false);
    }

    public int readInt() {
        return getInt(true);
    }

    private long getLong(boolean read) {
        if (isReadable(8)) {
            long res;
            UnsafeLinkedList.Node<ByteBuf> currentNode = this.currentNode;
            int currentOffset = this.currentOffset;
            int currentReadable = moveToValid();
            if (currentReadable >= 8) {
                res = safeGetCurrent().getLong(this.currentOffset);
                this.currentOffset += 8;
            } else {
                res = 0;
                int toRead = currentReadable;
                int hasRead = 0;
                while (toRead > 0) {
                    for (int i = 1; i <= toRead; ++i) {
                        res |= ((long) (safeGetCurrent().getByte(this.currentOffset) & 0xff)) << ((8 - hasRead - i) * 8);
                        ++this.currentOffset;
                    }
                    currentReadable -= toRead;
                    int remaining = 8 - hasRead - toRead;
                    hasRead += toRead;
                    currentReadable = moveToValid(currentReadable);
                    toRead = Math.min(currentReadable, remaining);
                }
            }
            if (read) {
                readerIndex += 8;
            } else {
                this.currentNode = currentNode;
                this.currentOffset = currentOffset;
            }
            return res;
        }
        throw new IndexOutOfBoundsException();
    }

    public long getLong() {
        return getLong(false);
    }

    public long readLong() {
        return getLong(true);
    }

    private void getBytes(byte[] output, int offset, int length, boolean read) {
        if (isReadable(length)) {
            UnsafeLinkedList.Node<ByteBuf> currentNode = this.currentNode;
            int currentOffset = this.currentOffset;
            int currentReadable = moveToValid();
            int toRead = Math.min(currentReadable, length);
            int hasRead = 0;
            while (toRead > 0) {
                safeGetCurrent().getBytes(this.currentOffset, output, offset + hasRead, toRead);
                this.currentOffset += toRead;
                currentReadable -= toRead;
                int remaining = length - hasRead - toRead;
                hasRead += toRead;
                currentReadable = moveToValid(currentReadable);
                toRead = Math.min(currentReadable, remaining);
            }
            if (read) {
                readerIndex += length;
            } else {
                this.currentNode = currentNode;
                this.currentOffset = currentOffset;
            }
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    public void getBytes(byte[] output, int offset, int length) {
        getBytes(output, offset, length, false);
    }

    public void readBytes(byte[] output, int offset, int length) {
        getBytes(output, offset, length, true);
    }

    public void getBuf(ByteBuf output, int length, boolean read) {
        if (isReadable(length)) {
            UnsafeLinkedList.Node<ByteBuf> currentNode = this.currentNode;
            int currentOffset = this.currentOffset;
            int currentReadable = moveToValid();
            int toRead = Math.min(currentReadable, length);
            int hasRead = 0;
            while (toRead > 0) {
                safeGetCurrent().getBytes(this.currentOffset, output, toRead);
                this.currentOffset += toRead;
                currentReadable -= toRead;
                int remaining = length - hasRead - toRead;
                hasRead += toRead;
                currentReadable = moveToValid(currentReadable);
                toRead = Math.min(currentReadable, remaining);
            }
            if (read) {
                readerIndex += length;
            } else {
                this.currentNode = currentNode;
                this.currentOffset = currentOffset;
            }
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    public void getBuf(ByteBuf output, int length) {
        getBuf(output, length, false);
    }

    public void readBuf(ByteBuf output, int length) {
        getBuf(output, length, true);
    }

    private ByteBuf getBuf(ByteBufAllocator allocator, int length, boolean read) {
        if (length < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (length == 0) {
            return Unpooled.EMPTY_BUFFER;
        }
        if (isReadable(length)) {
            CompositeByteBuf output = allocator.compositeBuffer();
            int currentReadable = moveToValid();
            UnsafeLinkedList.Node<ByteBuf> currentNode = this.currentNode;
            int currentOffset = this.currentOffset;
            int remaining = length;
            while (remaining > 0) {
                output.addComponent(true, this.currentNode.getData().retainedSlice());
                int size = Math.min(currentReadable, remaining);
                this.currentOffset += size;
                remaining -= size;
                currentReadable = moveToValid(currentReadable - size);
            }
            if (read) {
                readerIndex += length;
            } else {
                this.currentNode = currentNode;
                this.currentOffset = currentOffset;
            }
            return output.slice(currentOffset, length);
        }
        throw new IndexOutOfBoundsException();
    }

    public ByteBuf getBuf(ByteBufAllocator allocator, int length) {
        return getBuf(allocator, length, false);
    }

    public ByteBuf readBuf(ByteBufAllocator allocator, int length) {
        return getBuf(allocator, length, true);
    }

    public void release() {
        UnsafeLinkedList.Node<ByteBuf> node = buffers.getHead();
        while (node != null) {
            node.getData().release();
            node = node.getNext();
        }
        buffers.clear();
        currentNode = null;
        currentOffset = -1;
        markIndex = -1;
        readerIndex = writerIndex = 0;
    }
}
