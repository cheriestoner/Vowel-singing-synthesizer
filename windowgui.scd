s.boot;

(
SynthDef(\svf_test, {
	arg low=0.1, band=0.0, high=0.0, notch=0.0, peak=0.0,
	f0=110.0, volume=4.0, f1=150.0, f2=300, f3=2400;
    var source, sigF1, sigF2, sigF3;

	source = Blip.ar(SinOsc.kr(6.5, 0, 0.01, f0), f0, mul:volume);

    sigF1 = SVF.ar(source,
		f1, //(150,1000),    // cutoff
        0.6,           // q
        low, band, high, notch, peak, mul: 4);        // levels
    sigF2 = SVF.ar(sigF1,
        f2, //(300,2000),    // cutoff
        0.8,           // q
        low, band, high, notch, peak, mul: 4);        // levels
    sigF3 = SVF.ar(sigF2,
        f3,    // 2400 cutoff
        0.8,           // q
        low, band, high, notch, peak, mul: 4);        // levels
    Out.ar(0, sigF3 ! 2);
}).add; //play(addAction: \addToTail);
)

// gui
(

Window.closeAll;
w = Window("gui", Rect(
	Window.screenBounds.width/2-100,
	Window.screenBounds.height/2-100,
	400,
	400), scroll: true)
.front
.alwaysOnTop_(true);

w.view.decorator_(FlowLayout(w.bounds, 10@10, 20@10));

// ------------ Source ----------------

~srcView = CompositeView(w, 100@60)
.background_(Color.gray.alpha_(0.3))
.resize = 5;
~srcView.decorator_(FlowLayout(~srcView.bounds, 5@5, 5@5));

~srcLabel = StaticText(~srcView, 50@20)
.string_("Source");
~playbtn = Button(~srcView, 30@20)
.states_([
	["OFF", Color.black, Color.gray(0.8)],  // value=0
	["ON", Color.white, Color(0.2, 0.6, 1.0)]    // value=1
])
.action_({
	arg obj;
	if(
		obj.value == 1,
		{z = Synth(\svf_test).register;},
		{z.free}
	);
});
~f0Box = NumberBox(~srcView, 50@20) // foundamental freq
.valueAction_(110)
.clipLo_(100)
.clipHi_(4000)
//.decimals_(2)
.action_({
	arg obj;
	var f0;
	f0 = obj.value.postln;
	if(
		z.isPlaying,
		{z.set(\f0, f0);}
	);
});


// ------------- SVF and Formant Freqs -----------------
// formants
~formantsControl = Array.fill(5, {
	arg view;
	view = CompositeView(w, 360@50)
	.background_(Color.rand);

	view.decorator_(FlowLayout(view.bounds, 5@0, 5@5));
});

Array.fill(5, {
	arg i;
	var subView1, subView2, subView3;
	subView1 = CompositeView(~formantsControl[i], 100@50)
	.background_(Color.gray.alpha_(0.5));
	subView1.decorator_(FlowLayout(subView1.bounds, 5@5, 5@0));
	subView2 = CompositeView(~formantsControl[i], 120@50)
	.background_(Color.gray.alpha_(0.5));
	subView2.decorator_(FlowLayout(subView2.bounds, 5@5, 5@0));
	subView3 = CompositeView(~formantsControl[i], 120@50)
	.background_(Color.gray.alpha_(0.5));
	subView3.decorator_(FlowLayout(subView3.bounds, 5@5, 5@0));
	// subView 1: freqs
	StaticText(subView1, 50@20) // formant number
	.string_("Formant"++(i+1))
	.align_(\center);
	NumberBox(subView1, 50@20) // formant freq
	.action_({
		arg obj;
		var cutoff;
		cutoff = obj.value.postln;
		if(
			z.isPlaying,
			{z.set("f"++(i+1), cutoff);}
		);
	});
	StaticText(subView1, 20@20).string_("Hz"); //freq in Hertz


	// subView 2: filter types and output levels
	Array.fill(3, {
		Knob(subView2, 30@30);
	});
	StaticText(subView2, 30@20).string_("LP").align_(\center);
	StaticText(subView2, 30@20).string_("BP").align_(\center);
	StaticText(subView2, 30@20).string_("HP").align_(\center);

	// subView 3: notch and peak
	StaticText(subView3, 30@20).string_("Notch").align_(\center);
	~notchLv = Slider(subView3, 70@20)
	.action({
		arg obj;
		var lv;
		lv = obj.value;
		if(
			z.isPlaying,
			{z.set(\notch, lv);}
		);
	});
	StaticText(subView3, 30@20).string_("Peak").align_(\center);
	~peakLv = Slider(subView3, 70@20)
	.action({
		arg obj;
		var lv;
		lv = obj.value;
		if(
			z.isPlaying,
			{z.set(\peak, lv);}
		);
	});

});

/*
	NumberBox(~formantsControl[i], 50@20) // formant freq
	.action_({
		arg obj;
		var cutoff;
		cutoff = obj.value.postln;
		if(
			z.isPlaying,
			{z.set("f"++i, cutoff);}
		);
	});
	StaticText(~formantsControl[i], 20@20).string_("Hz"); //freq in Hertz
	Knob(~formantsControl[i], 30@30); // svf output level
	Knob(~formantsControl[i], 30@30);
	Knob(~formantsControl[i], 30@30);
	// notch and peak
	StaticText(~formantsControl[i], 30@20)
	.string_("Notch");
	Slider(~formantsControl[i], 15@40);
	StaticText(~formantsControl[i], 30@20)
	.string_("Peak");
	Slider(~formantsControl[i], 15@40);
});
*/

)

(
var view;
view = ~formantsControl[0].children[0];
view.children[1].valueAction_(150)
.clipLo_(150)
.clipHi_(1000)
.decimals_(2);
view = ~formantsControl[1].children[0];
view.children[1].valueAction_(300)
.clipLo_(300)
.clipHi_(2000)
.decimals_(2);
view = ~formantsControl[2].children[0];
view.children[1].valueAction_(2000)
.clipLo_(2000)
.clipHi_(4000)
.decimals_(2);
);

CompositeView(~formantsControl[0], 50@50);
a = ~formantsControl[0].children[0]
a.children[1].valueAction_(150)