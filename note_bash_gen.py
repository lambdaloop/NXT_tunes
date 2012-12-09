import os

filename = "tetris//tetris theme A notes bottom.txt"
output_name = "tetris//tetris_bottom.sh"

#uses program beep
#it's usage is:
#beep [-f freq] [-l length] [-r reps] [-d delay] 
#[-D delay] [-s] [-c] [--verbose | --debug] [-e device]

#length of a quarter note in milliseconds
length = 300

#ratio of frequency (ex: 0.5 cuts normal frequency of the notes by 1/2)
freq_ratio = 1

#play the song while generating?
play = False

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

notes_file.close()

output = open(output_name, "w")

output.write("#!/bin/bash\n")
output.write("#sudo modprobe pcspkr\n") #command to enable beeps

for note in song:
    
    if note[0] == "R":
        #output.write('echo {0} {1}\n'.format(note[0], note[1])) #don't print rest
        cmd = "beep -f 1 -l 0 -D {0}\n".format(note[1]*length)
        output.write(cmd) #rest

        if play:
            os.system(cmd)
    else:
        
        output.write('echo {0} {1}\n'.format(note[0], note[1])) #print note
  
        cmd = "beep -f {0} -l {1}\n".format(
               note_freq[note[0]]*freq_ratio, note[1]*length)
        output.write(cmd) #play note
        
        if play:
            print note[0], note[1] #print now
            os.system(cmd) #play now
        
        if wait_time:
            cmd = "beep -f 1 -l 0 -D {0}\n".format(wait_time*length)
            output.write(cmd)
            
            if play:
                    os.system(cmd)
                
print "Song generated"

output.close()
