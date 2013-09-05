package wangtiling;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
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
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
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

    Image tex = null, seamTex = null;
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
    int[][] diagonals =
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
    int[] opp =
    {
        1, 0, 3, 2
    };
    int TILES_X = 64, TILES_Y = 64;
    int[][] hseams = new int[TILES_Y][TILES_Y];
    int[][] vseams = new int[TILES_Y][TILES_Y];
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
        
        AffineTransform origT = g.getTransform();
        g.transform(AffineTransform.getTranslateInstance(ox,oy));
        
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
                drawTile(g, seamsVisible ? seamTex : tex,
                    rx, ry, p[0], p[1]);
            }
        }
        
        g.setTransform(origT);
    }
    
    public void drawTile(Graphics2D g, Image img, int rx, int ry,
        int px, int py)
    {
        g.drawImage(img, rx, ry, rx + tileW, ry + tileH, px * tileW,
            py * tileH, px * tileW + tileW, py * tileH + tileH, this);
    }
    
    public void wangAll()
    {
        random = new Random(seed);
        for (int i = 0; i < TILES_Y; i++)
        {
            for (int j = 0; j < TILES_X; j++)
            {
                hseams[i][j] = random.nextInt(2);
                vseams[i][j] = random.nextInt(2);
            }
        }
        for (int i = 0; i < TILES_Y; i++)
        {
            for (int j = 0; j < TILES_X; j++)
            {
                boolean[] sim = getSimilar(i, j);
                
                if (sim[UP]) hseams[i][j] = 1-hseams[i][j];
                if (sim[DOWN]) hseams[(i+1)%TILES_Y][j] = 1-hseams[(i+1)%TILES_Y][j];
                if (sim[LEFT]) vseams[i][j] = 1-vseams[i][j];
                if (sim[RIGHT]) vseams[i][(j+1)%TILES_X] = 1-vseams[i][(j+1)%TILES_X];
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
     * Builds integer out of bits.
     *
     * @param b the bits.
     * @return the bits concatenated.
     */
    public int unbit(int[] b)
    {
        int bits = 0;
        for (int i = 0; i < b.length; i++)
        {
            bits |= b[i] << (b.length - 1 - i);
        }
        return bits;
    }
    
    public boolean[] getSimilar(int i, int j)
    {
        boolean[] sim = {false,false,false,false};
        int x = getWrap(i,j);
        for (int d = 0; d < 4; d++)
        {
            if (getDir(i, j, directions[d]) == x)
                sim[d] = true;
        }
        return sim;
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
    public int getDir(int i, int j, int[] dir)
    {
        return getWrap(i + dir[0],j + dir[1]);
    }
    
    public int getWrap(int i, int j)
    {
        return getWang(wrap(i,TILES_Y),wrap(j,TILES_X));
    }
    
    public int getWang(int i, int j)
    {
        int[] b = new int[4];
        b[UP] = hseams[i][j];
        b[DOWN] = hseams[(i+1)%TILES_Y][j];
        b[LEFT] = vseams[i][j];
        b[RIGHT] = vseams[i][(j+1)%TILES_X];
        return hash[unbit(b)];
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
        int x = getDir(i, j, directions[dir]);
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
        seamTex = createSeamTex();
        wangAll();
    }
    
    private Image createSeamTex()
    {
        int w = tileW*4;
        int h = tileH*4;
        BufferedImage img = new BufferedImage(w, h,
            BufferedImage.OPAQUE);
            
        Graphics2D g = img.createGraphics();
        try
        {
            g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            
            AffineTransform origT = g.getTransform();
        
            g.drawImage(tex,0,0,this);
            for (int i = 0; i < 4; i++)
            {
                for (int j = 0; j < 4; j++)
                {
                    int rx = tileW * j;
                    int ry = tileH * i;
                    int x = i*4+j;
                    for (int d = 0; d < 4; d++)
                    {
                        Color c = borders[d][x] == 0 ? Color.RED : Color.BLUE;
                        g.setColor(c);
                        g.setStroke(new BasicStroke(3));
                        g.drawLine(
                            (int)(rx + seams[d][0]*tileW),
                            (int)(ry + seams[d][1]*tileH),
                            (int)(rx + seams[d][2]*tileW),
                            (int)(ry + seams[d][3]*tileH));
                    }
                }
            }
            for (int i = 0; i < 4; i++)
            {
                for (int j = 0; j < 4; j++)
                {
                    int x = i*4+j;
                    int rx = tileW * j;
                    int ry = tileH * i;
                    FontRenderContext frc = g.getFontRenderContext();
                    Font f = new Font("Segoe UI", Font.PLAIN, 14);
                    String s = new String(x+"");
                    TextLayout textTl = new TextLayout(s, f, frc);
                    Shape outline = textTl.getOutline(null);
                    Rectangle outlineBounds = outline.getBounds();
                    AffineTransform transform = g.getTransform();
                    transform.translate(
                        rx+tileW/2-outlineBounds.width/2,
                        ry + tileH/2 + outlineBounds.height/2);
                    g.transform(transform);
                    g.setColor(Color.BLACK);
                    g.setStroke(new BasicStroke(2));
                    g.draw(outline);
                    g.setColor(Color.WHITE);
                    g.fill(outline);
                    g.setTransform(origT);
                }
            }
        } finally
        {
            g.dispose();
        }
        
        return img;
    }

    private void error(String message)
    {
        JOptionPane.showMessageDialog(this, message, "Error!", JOptionPane.ERROR_MESSAGE);
    }
}
