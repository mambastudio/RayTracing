/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.accel.grid.offheap.base;

import coordinate.memory.NativeObject.Element;
import java.nio.ByteBuffer;
import raytracing.core.coordinate.Point3i;

/**
 *
 * @author jmburu
 */
public class NCell implements Element<NCell>{
    
    public Point3i min;     ///< Minimum bounding box coordinate
    public int begin;     ///< Index of the first reference
    public Point3i max;     ///< Maximum bounding box coordinate
    public int end;       ///< Past-the-end reference index

    public NCell() {}
    public NCell(Point3i min, int begin, Point3i max, int end)        
    {
        this.min = min.copy();
        this.begin = begin;
        this.max = max.copy();
        this.end = end;
    }

    @Override
    public int sizeOf() {
        return 32;
    }

    @Override
    public byte[] getBytes() {
        ByteBuffer buf = ByteBuffer.allocate(sizeOf());
        buf.putInt(min.x); buf.putInt(min.y); buf.putInt(min.z);
        buf.putInt(begin);
        buf.putInt(max.x); buf.putInt(max.y); buf.putInt(max.z);
        buf.putInt(end);
        return buf.array();
    }

    @Override
    public void putBytes(byte[] bytes) {
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        this.min = new Point3i(buf.getInt(), buf.getInt(), buf.getInt());
        this.begin = buf.getInt();
        this.max = new Point3i(buf.getInt(), buf.getInt(), buf.getInt());
        this.end = buf.getInt();
    }

    @Override
    public NCell newInstance() {
        return new NCell();
    }

    @Override
    public NCell copy() {
        return new NCell(min, begin, max, end);
    }
    
}
