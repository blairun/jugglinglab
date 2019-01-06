// NotationGUI.java
//
// Copyright 2018 by Jack Boyce (jboyce@gmail.com) and others

/*
    This file is part of Juggling Lab.

    Juggling Lab is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    Juggling Lab is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Juggling Lab; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package jugglinglab.notation;

import java.awt.*;
import java.awt.event.*;
import java.util.ResourceBundle;
import java.text.MessageFormat;
import javax.swing.*;
import javax.swing.event.*;

import jugglinglab.core.*;
import jugglinglab.generator.*;
import jugglinglab.jml.JMLPattern;
import jugglinglab.util.*;
import jugglinglab.view.View;


public class NotationGUI extends JPanel implements ActionListener {
    static final ResourceBundle guistrings = jugglinglab.JugglingLab.guistrings;
    static final ResourceBundle errorstrings = jugglinglab.JugglingLab.errorstrings;

    protected JTabbedPane jtp = null;
    protected JFrame parent = null;
    protected View animtarget = null;
    protected PatternList patlist = null;
    protected boolean patlisttab = false;

    protected int currentnum = -1;
    protected JButton juggle = null;
    protected JButton run = null;
    protected JLabel busy = null;

    // Execution limits for generator
    protected static final int max_patterns = 1000;
    protected static final double max_time = 15.0;


    public NotationGUI(JFrame parent) {
        this(parent, null, null, false);
    }

    public NotationGUI(JFrame p, View v, PatternList pl, boolean patlisttab) {
        this.parent = p;
        this.animtarget = v;
        this.patlist = pl;
        this.patlisttab = (pl == null) ? patlisttab : true;
    }

    public JButton getDefaultButton() {
        if (jtp == null)
            return null;
        if (jtp.getSelectedIndex() == 0)
            return this.juggle;
        else
            return this.run;
    }


    public JMenu createNotationMenu() {
        JMenu notationmenu = new JMenu(guistrings.getString("Notation"));
        ButtonGroup buttonGroup = new ButtonGroup();

        for (int i = 0; i < Notation.builtinNotations.length; i++) {
            JRadioButtonMenuItem notationitem = new JRadioButtonMenuItem(Notation.builtinNotations[i]);
            notationitem.setActionCommand("notation"+(i+1));
            notationitem.addActionListener(this);
            notationmenu.add(notationitem);
            buttonGroup.add(notationitem);
        }

        return notationmenu;
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        String command = ae.getActionCommand();

        try {
            if (command.startsWith("notation")) {
                try {
                    int num = Integer.parseInt(command.substring(8));
                    if (num != currentnum) {
                        setNotation(num);
                        if (parent != null)
                            parent.pack();
                        currentnum = num;
                    }
                } catch (NumberFormatException nfe) {
                    throw new JuggleExceptionInternal("Error in notation number coding");
                }
            }
        } catch (Exception e) {
            ErrorDialog.handleFatalException(e);
        }
    }

    // input is for example Notation.NOTATION_SITESWAP
    public void setNotation(int num) throws JuggleExceptionUser, JuggleExceptionInternal {
        if (num > Notation.builtinNotations.length)
            return;

        Notation not = null;
        NotationControl control = null;
        if (num != Notation.NOTATION_NONE) {
            try {
                not = Notation.getNotation(Notation.builtinNotations[num-1]);
                Class<?> nc = Class.forName("jugglinglab.notation."+Notation.builtinNotations[num-1].toLowerCase()+"NotationControl");
                control = (NotationControl)(nc.newInstance());
            } catch (JuggleExceptionUser je) {
                throw new JuggleExceptionInternal("Could not create notation \""+Notation.builtinNotations[num-1]+"\"");
            } catch (ClassNotFoundException cnfe) {
                throw new JuggleExceptionUser("Could not find "+Notation.builtinNotations[num-1].toLowerCase()+"NotationControl class");
            } catch (IllegalAccessException iae) {
            } catch (InstantiationException ie) {
            }
        }

        if (jtp != null)
            remove(jtp);
        jtp = new JTabbedPane();

        PatternList pl = patlist;

        if (control != null) {
            final Notation fnot = not;
            final NotationControl fcontrol = control;

            JPanel np1 = new JPanel();
            np1.setLayout(new BorderLayout());
            np1.add(control, BorderLayout.PAGE_START);
            JPanel np2 = new JPanel();
            np2.setLayout(new FlowLayout(FlowLayout.TRAILING));
            JButton nbut1 = new JButton(guistrings.getString("Defaults"));
            nbut1.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    try {
                        fcontrol.resetNotationControl();
                    } catch (Exception e) {
                        ErrorDialog.handleFatalException(e);
                    }
                }
            });
            np2.add(nbut1);
            this.juggle = new JButton(guistrings.getString("Juggle"));
            this.juggle.setDefaultCapable(true);
            juggle.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    PatternWindow jaw2 = null;
                    try {
                        String p = fcontrol.getPattern();
                        JMLPattern pat = fnot.getJMLPattern(p);
                        String handsname = fcontrol.getHandsName();
                        if (handsname != null)
                            pat.setTitle(pat.getTitle() + " " + handsname);

                        if (animtarget != null)
                            animtarget.restartView(pat, new AnimationPrefs());
                        else
                            jaw2 = new PatternWindow(pat.getTitle(), pat, new AnimationPrefs());
                    } catch (JuggleExceptionUser je) {
                        if (jaw2 != null)
                            jaw2.dispose();
                        new ErrorDialog(NotationGUI.this, je.getMessage());
                    } catch (Exception e) {
                        if (jaw2 != null)
                            jaw2.dispose();
                        ErrorDialog.handleFatalException(e);
                    }
                }
            });
            np2.add(juggle);
            np1.add(np2, BorderLayout.PAGE_END);

            jtp.addTab(guistrings.getString("Pattern_entry"), np1);

            final Generator gen = Generator.getGenerator(Notation.builtinNotations[num-1]);

            if (gen != null) {
                if ((pl == null) && patlisttab)
                    pl = new PatternList(animtarget);
                final PatternList plf = pl;

                JPanel p1 = new JPanel();
                p1.setLayout(new BorderLayout());
                p1.add(gen.getGeneratorControls(), BorderLayout.PAGE_START);
                JPanel p2 = new JPanel();
                p2.setLayout(new FlowLayout(FlowLayout.TRAILING));
                JButton but1 = new JButton(guistrings.getString("Defaults"));
                but1.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        gen.resetGeneratorControls();
                    }
                });
                p2.add(but1);
                this.run = new JButton(guistrings.getString("Run"));
                run.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {

                        Thread t = new Thread() {
                            public void run() {
                                busy.setVisible(true);
                                run.setEnabled(false);
                                PatternListWindow pw = null;
                                try {
                                    gen.initGenerator();
                                    GeneratorTarget pwot = null;
                                    if (plf != null) {
                                        plf.clearList();
                                        pwot = new GeneratorTarget(plf);
                                        //jtp.setSelectedComponent(plf);
                                    } else {
                                        pw = new PatternListWindow(fnot.getName()+" "+guistrings.getString("Patterns"));
                                        pwot = new GeneratorTarget(pw);
                                    }
                                    gen.runGenerator(pwot, max_patterns, max_time);
                                    if (plf != null)
                                        jtp.setSelectedComponent(plf);
                                } catch (JuggleExceptionDone ex) {
                                    if (plf != null)
                                        jtp.setSelectedComponent(plf);
                                    Component parent = pw;
                                    if (pw == null)
                                        parent = plf;
                                    new LabelDialog(parent, guistrings.getString("Generator_stopped_title"), ex.getMessage());
                                } catch (JuggleExceptionUser ex) {
                                    if (pw != null)
                                        pw.dispose();
                                    new ErrorDialog(NotationGUI.this, ex.getMessage());
                                } catch (Exception e) {
                                    if (pw != null)
                                        pw.dispose();
                                    ErrorDialog.handleFatalException(e);
                                }

                                busy.setVisible(false);
                                run.setEnabled(true);
                            }
                        };
                        t.start();
                    }
                });
                p2.add(run);

                busy = new JLabel(guistrings.getString("Processing"));
                busy.setVisible(false);
                JPanel p3 = new JPanel();
                p3.setLayout(new BorderLayout());
                JPanel p4 = new JPanel();
                GridBagLayout gb = new GridBagLayout();
                p4.setLayout(gb);
                p4.add(busy);
                gb.setConstraints(busy, make_constraints(GridBagConstraints.LINE_START, 0, 0, new Insets(0,10,0,0)));
                p3.add(p4, BorderLayout.LINE_START);
                p3.add(p2, BorderLayout.LINE_END);

                p1.add(p3, BorderLayout.PAGE_END);

                // Change the default button when the tab changes
                jtp.addChangeListener(new ChangeListener() {
                    public void stateChanged(ChangeEvent e) {
                        getRootPane().setDefaultButton(getDefaultButton());
                    }
                });

                jtp.addTab(guistrings.getString("Generator"), p1);
            }
        }

        if (pl != null) {
            jtp.addTab(guistrings.getString("Pattern_list_tab"), pl);
            if (patlist != null)
                jtp.setSelectedComponent(pl);       // if we loaded from a file
        }

        setLayout(new BorderLayout());
        add(jtp, BorderLayout.CENTER);

        GridBagLayout gb = new GridBagLayout();
        setLayout(gb);
        add(jtp);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridwidth = gbc.gridheight = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0,0,0,0);
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gb.setConstraints(jtp, gbc);

        if (parent != null)
            parent.getRootPane().setDefaultButton(getDefaultButton());
    }

    protected static GridBagConstraints make_constraints(int location, int gridx, int gridy, Insets ins) {
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.anchor = location;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridheight = gbc.gridwidth = 1;
        gbc.gridx = gridx;
        gbc.gridy = gridy;
        gbc.insets = ins;
        gbc.weightx = gbc.weighty = 0.0;
        return gbc;
    }
}
