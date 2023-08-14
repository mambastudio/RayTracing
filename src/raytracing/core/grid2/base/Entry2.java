/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.core.grid2.base;

import coordinate.memory.NativeInteger.IntElement;
import coordinate.memory.NativeObject.Element;
import coordinate.utility.BitUtility;
import java.nio.ByteBuffer;

/**
 *
 * @author jmburu
 */
public class Entry2 implements Element<Entry2>, IntElement<Entry2> {
    public static final int LOG_DIM_BITS = 2;
    public static final int BEGIN_BITS = 32 - LOG_DIM_BITS;
    
    public int log_dim;    ///< Logarithm of the dimensions of the entry (0 for leaves)
    public int begin;      ///< Next entry index (cell index for leaves)
    
    public Entry2(){
        log_dim = 0;
        begin = 0;
    }
    
    public Entry2(int value)
    {
        setInt(value);
    }
    
    public Entry2(int log_dim, int begin)
    {
        this.log_dim = log_dim;
        this.begin = begin;
    }
    
    @Override
    public int sizeOf() {
        return 8;
    }

    @Override
    public byte[] getBytes() {
        ByteBuffer buf = ByteBuffer.allocate(sizeOf());
        buf.putInt(log_dim);
        buf.putInt(begin);
        return buf.array();
    }

    @Override
    public void putBytes(byte[] bytes) {
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        this.log_dim = buf.getInt();
        this.begin = buf.getInt();       
    }

    @Override
    public Entry2 newInstance() {
        return new Entry2();
    }

    @Override
    public Entry2 copy() {
        return new Entry2(log_dim, begin);
    }

    @Override
    public int getInt() {
        int value = 0;
        value = BitUtility.apply_bits_at(0, log_dim, value);
        value = BitUtility.apply_bits_at(2, begin, value);
        return value;
    }

    @Override
    public void setInt(int value) {
        log_dim = BitUtility.get_bits_at(0, value, LOG_DIM_BITS);
        begin = BitUtility.get_bits_at(2, value, BEGIN_BITS);
    }
    
}
