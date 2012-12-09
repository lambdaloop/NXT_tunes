filename = "mario theme notes.txt"



wait_time = 0
ratio = 2**(1/12.0)
freq  = 27.5 * (ratio**-9)
notes = ['C', 'C+', 'D', 'D+', 'E', 'F', 'F+', 'G', 'G+', 'A', 'A+', 'B']
all_notes = []
all_freq  = []

for num in range(8):
    for note in notes:
        all_notes.append(note+str(num))
        all_freq.append(int(round(freq)))
        freq *= ratio

note_freq = dict(zip(all_notes, all_freq))



notes_file = open(filename, "r")

song = [] 
for row in notes_file:
    s = row.strip('\n').split(" ")
    if len(s) == 2:
        s[1] = float(s[1])
        song.append(s)
        #print s


output = open("mario.java", "w")


for note in song:
    if note[0] == "R":
        output.write("Tone.playTone(0, {0});\n".format(note[1]/1.67))
    else:
    	output.write('System.out.println("{0} " + {1});\n'.format(note[0], note[1]))
        output.write("Tone.playTone({0}, {1});\n".format(
            note_freq[note[0]], note[1]/1.67))
        if wait_time:
            output.write("Tone.playTone(0, {0});\n".format(wait_time/1.67))

print "Song generated"

output.close()
