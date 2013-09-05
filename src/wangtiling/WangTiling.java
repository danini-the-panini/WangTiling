package wangtiling;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 *
 * @author Daniel
 */
public class WangTiling extends JPanel
{
    public static final Font FONT = new Font("Segoe UI", Font.PLAIN, 24);
    public static final String MESSAGE = "Drag an image here";
    public static final int DRAG_BOX_SIZE = 50;

    Image tex = null;
    int tileW, tileH;
    long seed;
    int[][] borders =
    {
        {
            1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0
        },
        {
            0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0
        },
        {
            0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0
        },
        {
            0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1
        }
    };
    int[] hash =
    {
        12, 13, 15, 14, 0, 1, 3, 2, 8, 9, 11, 10, 4, 5, 7, 6
    };
    int UP = 0;
    int DOWN = 1;
    int LEFT = 2;
    int RIGHT = 3;
    float seamWidth = 3f;
    boolean seamsVisible = false;
    float[][] seams = {
       {0,1,1,1},
       {0,0,1,0},
       {1,0,1,1},
       {0,0,0,1}
    };
    int[][] directions =
    {
        {
            -1, 0
        },
        {
            1, 0
        },
        {
            0, -1
        },
        {
            0, 1
        }
    };
    int[][] diag =
    {
        {
            -1, -1
        },
        {
            -1, 1
        },
        {
            1, 1
        },
        {
            1, -1
        }
    };
    int TILES_X = 64, TILES_Y = 64;
    int[][] tiles = new int[TILES_X][TILES_Y];
    Random random;
    
    int mx, my;
    int ox, oy;

    public WangTiling()
    {
        seed = System.currentTimeMillis();

        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                seamsVisible = !seamsVisible;
                repaint();
            }
            
            @Override
            public void mousePressed(MouseEvent e)
            {
                mx = e.getX();
                my = e.getY();
            }
        });
        
        addMouseMotionListener(new MouseAdapter()
        {
            @Override
            public void mouseDragged(MouseEvent e)
            {
                ox += (e.getX() - mx);
                oy += (e.getY() - my);
                mx = e.getX();
                my = e.getY();
                repaint();
            }
        });

        DropTarget dt = new DropTarget(this, new DropTargetAdapter()
        {
            @Override
            public void drop(DropTargetDropEvent dtde)
            {
                try
                {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                    Transferable t = dtde.getTransferable();
                    if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
                    {
                        String flist = t.getTransferData(DataFlavor.javaFileListFlavor).toString();
                        if (flist == null || flist.isEmpty())
                            return;
                        flist = flist.substring(1, flist.length()-1);
                        String[] farray = flist.split(", ");
                        if (farray.length < 1)
                            return;
                        String lastFile = farray[farray.length-1];
                        loadImage(lastFile);
                        repaint();
                    }
                } catch (UnsupportedFlavorException | IOException ex)
                {
                    error("Error loading file: " + ex.getMessage());
                }
            }
        });
        dt.setActive(true);
        setDropTarget(dt);
    }

    @Override
    public void paint(Graphics gfx)
    {
        Graphics2D g = (Graphics2D) gfx;
        
        g.setBackground(Color.WHITE);
        g.clearRect(0, 0, getWidth(), getHeight());
        
        g.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
            
        AffineTransform origT = g.getTransform();
        g.transform(AffineTransform.getTranslateInstance(ox,oy));
        
        // draw drop text if no texture is loaded
        g.setFont(FONT);
        g.setColor(Color.GRAY);
        if (tex == null)
        {
            int stringWidth = g.getFontMetrics().stringWidth(MESSAGE);
            int stringHeight =  g.getFontMetrics().getHeight();
            
            int boxWidth = stringWidth + DRAG_BOX_SIZE;
            int boxHeight = stringHeight + DRAG_BOX_SIZE;
            
            g.drawString(MESSAGE,
                    getWidth()/2 - stringWidth/2,
                    getHeight()/2 -stringHeight/2 + 25);
            
            g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_ROUND, 0, new float[]{5,5}, 1));
            
            g.drawRoundRect(getWidth()/2 - boxWidth/2, getHeight()/2
                -boxHeight/2, boxWidth, boxHeight, DRAG_BOX_SIZE,
                DRAG_BOX_SIZE);
            
            return;
        }
        
        g.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setStroke(new BasicStroke(seamWidth));
        
        int er = getHeight()/tileH+3;
        int ec = getWidth()/tileW+3;
        int ei = -oy/tileH-1;
        int ej = -ox/tileW-1;
        for (int row = 0; row < er; row++)
        {
            for (int col = 0; col < ec; col++)
            {
                int i = ei+row;
                int j = ej+col;
                int x = getWrap(i, j);
                int[] p = indexToPoint(x);
                int rx = j * tileW, ry = i * tileH;
                g.drawImage(tex, rx, ry, rx + tileW, ry + tileH, p[0] * tileW, p[1] * tileH, p[0] * tileW + tileW, p[1] * tileH + tileH, this);
                
                if (seamsVisible)
                {
                    for (int d = 0; d < 4; d++)
                    {
                        Color c = borders[d][x] == 0 ? Color.RED : Color.BLUE;
                        g.setColor(c);
                        g.drawLine(
                            (int)(rx + seams[d][0]*tileW),
                            (int)(ry + seams[d][1]*tileH),
                            (int)(rx + seams[d][2]*tileW),
                            (int)(ry + seams[d][3]*tileH));
                    }
                }
            }
        }
        
        g.setTransform(origT);
    }
    
    public void wangAll()
    {
        random = new Random(seed);
        for (int i = 0; i < tiles.length; i++)
        {
            for (int j = 0; j < tiles[i].length; j++)
            {
                tiles[i][j] = -1;
            }
        }
        for (int i = 0; i < tiles.length; i++)
        {
            for (int j = 0; j < tiles[i].length; j++)
            {
                tiles[i][j] = wang(i, j);
            }
        }
    }

    public int[] indexToPoint(int x)
    {
        return new int[]
        {
            x % 4, x / 4
        };
    }

    /**
     * Calculates the Wang tile for the position.
     *
     * @param i
     * @param j
     * @return
     */
    public int wang(int i, int j)
    {
        int bits;
//        do
//        {
            bits = 0;
            // build integer out of bits that represent border colour
            for (int d = 0; d < 4; d++)
            {
                bits |= getBorder(i, j, d) << (3 - d);
            }
//        } while (sameAsNeighbour(i, j, hash[bits]));
        return hash[bits];
    }

    /**
     * Checks if the given tile value is the same as it's neighbours and
     * diagonals. This is for avoiding repeats.
     *
     * @param i
     * @param j
     * @param x value of tile
     * @return
     */
    public boolean sameAsNeighbour(int i, int j, int x)
    {
        for (int d = 0; d < 4; d++)
        {
            if (get(i, j, directions[d]) == x)
            {
                return true;
            }
            if (get(i, j, diag[d]) == x)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Wraps the index a around length b.
     *
     * @param a
     * @param b
     * @return
     */
    public int wrap(int a, int b)
    {
        int x = a % b;
        if (x < 0)
        {
            x += b;
        }
        return x;
    }

    /**
     * Gets the value of the tile relative to the given indices.
     *
     * @param i
     * @param j
     * @param dir the relative direction
     * @return
     */
    public int get(int i, int j, int[] dir)
    {
        return getWrap(i + dir[0],j + dir[1]);
    }
    
    public int getWrap(int i, int j)
    {
        return tiles[wrap(i,tiles.length)][wrap(j,tiles[0].length)];
    }

    /**
     * Gets the colour of the given edge.
     *
     * @param i
     * @param j
     * @param dir
     * @return
     */
    public int getBorder(int i, int j, int dir)
    {
        int x = get(i, j, directions[dir]);
        if (x < 0)
        {
            return random();
        }
        return borders[dir][x];
    }

    public int random()
    {
        return random.nextInt(2);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        JFrame frame = new JFrame("Wang!");
        frame.add(new WangTiling());
        frame.setSize(500, 500);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private void loadImage(String fileName) throws IOException
    {
        Image newTex = ImageIO.read(new File(fileName));
        if (newTex == null)
        {
            error("That is not an image!");
            return;
        }
        tex = newTex;
        seed = System.currentTimeMillis();
        tileW = tex.getWidth(this) / 4;
        tileH = tex.getHeight(this) / 4;
        ox = -(tileW * TILES_X - getWidth()) / 2;
        oy = -(tileH * TILES_Y - getHeight()) / 2;
        wangAll();
    }

    private void error(String message)
    {
        JOptionPane.showMessageDialog(this, message, "Error!", JOptionPane.ERROR_MESSAGE);
    }
}
